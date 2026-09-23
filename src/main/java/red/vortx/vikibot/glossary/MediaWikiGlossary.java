package red.vortx.vikibot.glossary;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class MediaWikiGlossary {

    public static final URI API = URI.create("https://he.minecraft.wiki/api.php");

    private static final String QUERY = "?action=parse&page=%D7%A7%D7%94%D7%99%D7%9C%D7%94%3A%D7%9E%D7%99%D7%9C%D7%95%D7%9F&prop=wikitext&format=json&formatversion=2";
    public static final String USER_AGENT = "HebrewWikiBot";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final URI api;

    public MediaWikiGlossary(URI api) {
        this.httpClient = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
        this.api = api;
    }

    public CompletableFuture<Glossary> fetch() {
        HttpRequest request = HttpRequest.newBuilder(api.resolve(api.getPath() + QUERY))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException("MediaWiki returned HTTP " + response.statusCode());
                    }
                    JsonNode root = mapper.readTree(response.body());
                    JsonNode wikitext = root.path("parse").path("wikitext");
                    if (!wikitext.isString()) {
                        throw new IllegalArgumentException("MediaWiki did not return glossary wiki text");
                    }
                    return Glossary.parse(wikitext.stringValue());
                });
    }

}
