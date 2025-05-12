package com.jagrosh.jmusicbot.audio;

import com.dunctebot.sourcemanagers.DuncteBotSources;
import com.github.topi314.lavasearch.SearchManager;
import com.github.topi314.lavasrc.mirror.DefaultMirroringAudioTrackResolver;
import com.github.topi314.lavasrc.spotify.SpotifySourceManager;
import com.jagrosh.jmusicbot.Bot;
import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.nico.NicoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import java.util.List;

/**
 * Manages audio sources and players for the bot, including LavaSrc providers.
 */
public class PlayerManager extends DefaultAudioPlayerManager {

    private final Bot bot;

    public PlayerManager(Bot bot) {
        this.bot = bot;
    }

    public void init() {
        // Existing transforms and sources
        TransformativeAudioSourceManager.createTransforms(bot.getConfig().getTransforms())
            .forEach(this::registerSourceManager);

        YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager(true);
        yt.setPlaylistPageCount(bot.getConfig().getMaxYTPlaylistPages());
        registerSourceManager(yt);

        registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        registerSourceManager(new BandcampAudioSourceManager());
        registerSourceManager(new VimeoAudioSourceManager());
        registerSourceManager(new TwitchStreamAudioSourceManager());
        registerSourceManager(new BeamAudioSourceManager());
        registerSourceManager(new GetyarnAudioSourceManager());
        registerSourceManager(new NicoAudioSourceManager());
        registerSourceManager(new HttpAudioSourceManager(MediaContainerRegistry.DEFAULT_REGISTRY));

        AudioSourceManagers.registerLocalSource(this);
        DuncteBotSources.registerAll(this, "en-US");

        // === Integrazione LavaSrc ===

        // 1) Spotify
        String clientId = bot.getConfig().getClientId();
        String clientSecret = bot.getConfig().getClientSecret();
        String countryCode = bot.getConfig().getCountryCode();
        System.out.println(clientId + clientSecret + countryCode);
        SpotifySourceManager spotify = new SpotifySourceManager(
            clientId,
            clientSecret,
            countryCode,
            this,
             new DefaultMirroringAudioTrackResolver(new String[0])
        );
        registerSourceManager(spotify);

        // (Opzionale) LavaSearch → ricerca su Spotify
        SearchManager searchManager = new SearchManager();
        searchManager.registerSearchManager(spotify);

    }

    public Bot getBot() {
        return bot;
    }

    public boolean hasHandler(Guild guild) {
        return guild.getAudioManager().getSendingHandler() != null;
    }

    public AudioHandler setUpHandler(Guild guild) {
        AudioHandler handler;
        if (guild.getAudioManager().getSendingHandler() == null) {
            AudioPlayer player = createPlayer();
            player.setVolume(bot.getSettingsManager().getSettings(guild).getVolume());
            handler = new AudioHandler(this, guild, player);
            player.addListener(handler);
            guild.getAudioManager().setSendingHandler(handler);
        } else {
            handler = (AudioHandler) guild.getAudioManager().getSendingHandler();
        }
        return handler;
    }
}
