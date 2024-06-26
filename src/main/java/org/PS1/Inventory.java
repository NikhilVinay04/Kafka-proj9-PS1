package org.PS1;
// Provides the schema for the Inventory table as specified in the ORM file
public class Inventory
{
    int itemID;
    String itemName;
    double quantity;
    long date;
    public Inventory(int itemID, String itemName, double quantity, long date)
    {
        this.itemID = itemID;
        this.itemName = itemName;
        this.quantity = quantity;
        this.date = date;
    }

    public int getItemID() {
        return itemID;
    }
    public String getItemName() {
        return itemName;
    }
    public double getQuantity() {
        return quantity;
    }
    public long getDate() {
        return date;
    }
}
