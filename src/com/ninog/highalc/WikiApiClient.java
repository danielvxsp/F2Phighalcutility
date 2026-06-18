import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class WikiApiClient {

    private static final String USER_AGENT = "F2PHighAlchFinder - @YourDiscordOrEmail";
    private final HttpClient client;
    private int currentNatPrice = 0;

    public WikiApiClient() {
        this.client = HttpClient.newHttpClient();
    }

    public int getCurrentNatPrice() {
        return currentNatPrice;
    }

    public List<AlchItem> fetchTopAlchs(int minVolume, int maxResults) throws Exception {
        // first get the data from the API
        JsonArray mappingData = fetchJsonArray("https://prices.runescape.wiki/api/v1/osrs/mapping");
        JsonObject pricesData = fetchJsonObject("https://prices.runescape.wiki/api/v1/osrs/latest");
        JsonObject volumesData = fetchJsonObject("https://prices.runescape.wiki/api/v1/osrs/volumes");

        // get the nature rune price to add to GE buy price
        if (pricesData.has("561")) {
            JsonObject natObj = pricesData.getAsJsonObject("561");
            if (natObj.has("high") && !natObj.get("high").isJsonNull()) {
                currentNatPrice = natObj.get("high").getAsInt();
            }
        }

        // process the items
        List<AlchItem> results = new ArrayList<>();
        for (JsonElement element : mappingData) {
            JsonObject item = element.getAsJsonObject();
            boolean isMembers = item.has("members") && item.get("members").getAsBoolean();
            int highAlch = getIntOrZero(item, "highalch");

            if (!isMembers && highAlch > 0) {
                String id = item.get("id").getAsString();
                
                if (pricesData.has(id)) {
                    JsonObject priceInfo = pricesData.getAsJsonObject(id);
                    int buyPrice = getIntOrZero(priceInfo, "high");

                    if (buyPrice > 0) {
                        int volume = getIntOrZero(volumesData, id);

                        if (volume >= minVolume) {
                            int profit = highAlch - (buyPrice + currentNatPrice);
                            int limit = getIntOrZero(item, "limit");
                            results.add(new AlchItem(id, item.get("name").getAsString(), buyPrice, highAlch, profit, limit, volume));
                        }
                    }
                }
            }
        }

        // sort by most profit by default
        results.sort((a, b) -> Integer.compare(b.profit, a.profit));
        
        if (maxResults == -1) {
            return results;
        } else {
            return results.subList(0, Math.min(results.size(), maxResults));
        }
    }

    private JsonArray fetchJsonArray(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", USER_AGENT).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(res.body()).getAsJsonArray();
    }

    private JsonObject fetchJsonObject(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", USER_AGENT).build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        return JsonParser.parseString(res.body()).getAsJsonObject().getAsJsonObject("data");
    }

    private int getIntOrZero(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsInt();
        }
        return 0;
    }
}
