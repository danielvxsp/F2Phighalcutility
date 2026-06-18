public class AlchItem {
    public final String id;
    public final String name;
    public final int buyPrice;
    public final int alchValue;
    public final int profit;
    public final int limit;
    public final int volume;

    public AlchItem(String id, String name, int buyPrice, int alchValue, int profit, int limit, int volume) {
        this.id = id;
        this.name = name;
        this.buyPrice = buyPrice;
        this.alchValue = alchValue;
        this.profit = profit;
        this.limit = limit;
        this.volume = volume;
    }
}
