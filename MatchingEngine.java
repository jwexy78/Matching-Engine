import java.util.Scanner;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Comparator;
import java.util.Collections;

public class MatchingEngine 
{
    public static BufferedWriter out;
    public static void main(String[] args)
    {
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(args[0]));
            String writeToFile = args[0].substring(0,args[0].lastIndexOf("/")+1) +"fills.txt";
            
            File writeTo = new File(writeToFile);
            
            if(!writeTo.exists())
                writeTo.createNewFile();
                
            FileWriter fw = new FileWriter(writeTo.getAbsoluteFile());
            out = new BufferedWriter(fw);
            
            String line;
            HashMap<String,Market> markets = new HashMap<String,Market>();
            while((line = reader.readLine()) != null)
            {
                String[] spl = line.split(",");
                
                if(spl.length < 6)
                    break;
                
                Order newOrder = new Order();
                newOrder.type = spl[0];
                newOrder.id = spl[1];
                newOrder.symbol = spl[2];
                newOrder.buy = spl[3].equals("B");
                newOrder.quantity = Integer.parseInt(spl[4]);
                newOrder.price = Double.parseDouble(spl[5]);
                if(spl.length > 6)
                {
                    newOrder.oldQuantity = Integer.parseInt(spl[6]);
                    newOrder.oldPrice = Double.parseDouble(spl[7]);
                }
                
                if(!markets.containsKey(newOrder.symbol))
                {
                    Market newMarket = new Market();
                    markets.put(newOrder.symbol, newMarket);
                }
                markets.get(newOrder.symbol).processOrder(newOrder);
            }
            
            out.close();
            reader.close();
            
        }
        catch(Exception e)
        {
            System.out.println("File Not Found / Other Error Occured");
            System.out.println(e.getStackTrace());
            e.printStackTrace();
            return;
        }
    }
}

class Market
{
    public ArrayList<Order> buyOrders;
    public ArrayList<Order> sellOrders;
    
    public static Comparator<Order> comp;
    
    public Market()
    {
        buyOrders = new ArrayList<Order>(10000);
        sellOrders = new ArrayList<Order>(10000);
        comp = new Comparator<Order>() 
        {
            public int compare(Order u1, Order u2) 
            {
                if(u1.buy)
                    return (int)(10000*(u2.price - u1.price));
                else
                    return (int)(10000*(u1.price - u2.price));
            }
        };
    }
    public void processOrder(Order newOrder)
    {
        if(newOrder.type.equals("ORDER"))
        {
            if(!tryToFill(newOrder))
            {
                addOrderToBook(newOrder);
            }
        }
        else if(newOrder.type.equals("CANCEL"))
        {
            cancelOrder(newOrder);
        }
        else if(newOrder.type.equals("REPLACE"))
        {
            replaceOrder(newOrder);
        }
    }
    /**
     * Tries to fill an order/
     * Should run in O(n) where n is num orders that this new order fills
     */
    public boolean tryToFill(Order order)
    {
        if(order.buy)
        {    
            for(int i = 0; i < sellOrders.size(); i++)
            {
                if(order.price >= sellOrders.get(i).price)
                {
                    if(order.quantity == sellOrders.get(i).quantity)
                    {
                        writeToFile("FILL "+ order.symbol +" " +order.id + " " +sellOrders.get(i).id +" " +order.quantity +" " +sellOrders.get(i).price);
                        sellOrders.remove(i);
                        i--;
                        return true;
                    }
                    else if(order.quantity < sellOrders.get(i).quantity)
                    {
                        writeToFile("FILL "+ order.symbol +" " +order.id + " " +sellOrders.get(i).id +" " +order.quantity +" " +sellOrders.get(i).price);
                        sellOrders.get(i).quantity -= order.quantity;
                        return true;
                    }
                    else
                    {
                        writeToFile("FILL "+ order.symbol +" " +order.id + " " +sellOrders.get(i).id +" " +sellOrders.get(i).quantity +" " +sellOrders.get(i).price);
                        order.quantity -= sellOrders.get(i).quantity;
                        sellOrders.remove(i);
                        i--;
                    }
                }
                else
                {
                    return false;
                }
            }
        }
        else
        {    
            for(int i = 0; i < buyOrders.size(); i++)
            {
                if(order.price <= buyOrders.get(i).price)
                {
                    if(order.quantity == buyOrders.get(i).quantity)
                    {
                        writeToFile("FILL "+ order.symbol +" " +buyOrders.get(i).id +" "+order.id + " "+order.quantity +" " +buyOrders.get(i).price);
                        buyOrders.remove(i);
                        i--;
                        return true;
                    }
                    else if(order.quantity < buyOrders.get(i).quantity)
                    {
                        writeToFile("FILL "+ order.symbol +" " +buyOrders.get(i).id +" "+order.id + " "+order.quantity +" " +buyOrders.get(i).price);
                        buyOrders.get(i).quantity -= order.quantity;
                        return true;
                    }
                    else
                    {
                        writeToFile("FILL "+ order.symbol +" " +buyOrders.get(i).id +" "+order.id + " "+buyOrders.get(i).quantity +" " +buyOrders.get(i).price);
                        order.quantity -= buyOrders.get(i).quantity;
                        buyOrders.remove(i);
                        i--;
                    }
                }
                else
                {
                    return false;
                }
            }
        }
        return false;
    }
    
    /**
     * Writes a line to the output file
     */
    public boolean writeToFile(String out)
    {
        try 
        {
            if(out.substring(out.length()-2,out.length()).equals(".0"))
            {
                out = out.substring(0,out.length()-2);
            }
            MatchingEngine.out.write(out);
            MatchingEngine.out.newLine();
            return true;
        } catch (IOException e) 
        {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Adds an unfilled order to the book
     */
    public void addOrderToBook(Order order)
    {
        if(order.buy)
        {
            int index = Collections.binarySearch(buyOrders, order, comp);
            if(index >= 0)
            {
                for(int i = index; i < buyOrders.size(); i++)
                {
                    if(buyOrders.get(i).price < order.price)
                    {
                        buyOrders.add(i, order);
                        return;
                    }
                }
                buyOrders.add(order);
            }
            else
            {
                buyOrders.add(index * -1 - 1, order);
            }
        }
        else
        {
            int index = Collections.binarySearch(sellOrders, order, comp);
            if(index >= 0)
            {
                for(int i = index; i < sellOrders.size(); i++)
                {
                    if(sellOrders.get(i).price > order.price)
                    {
                        sellOrders.add(i, order);
                        return;
                    }
                }
                sellOrders.add(order);
            }
            else
            {
                sellOrders.add(index * -1 - 1, order);
            }
        }
    }
    
    public boolean cancelOrder(Order order)
    {
        if(order.buy)
        {
            int index = Collections.binarySearch(buyOrders, order, comp);
            if(index >= 0)
            {
                for(int i = index; i < buyOrders.size(); i++)
                {
                    if(order.id.equals(buyOrders.get(i).id))
                    {
                        buyOrders.remove(i);
                        return true;
                    }
                    if(order.price != buyOrders.get(i).price)
                    {
                        break;
                    }
                }
                for(int i = index - 1; i >= 0; i--)
                {
                    if(order.id.equals(buyOrders.get(i).id))
                    {
                        buyOrders.remove(i);
                        return true;
                    }
                    if(order.price != buyOrders.get(i).price)
                    {
                        break;
                    }
                }
            }
        }
        else
        {
            int index = Collections.binarySearch(sellOrders, order, comp);
            if(index >= 0)
            {
                for(int i = index; i < sellOrders.size(); i++)
                {
                    if(order.id.equals(sellOrders.get(i).id))
                    {
                        sellOrders.remove(i);
                        return true;
                    }
                    if(order.price != sellOrders.get(i).price)
                    {
                        break;
                    }
                }
                for(int i = index - 1; i >= 0; i--)
                {
                    if(order.id.equals(sellOrders.get(i).id))
                    {
                        sellOrders.remove(i);
                        return true;
                    }
                    if(order.price != sellOrders.get(i).price)
                    {
                        break;
                    }
                }
            }
        }
        return false;
    }
    
    public boolean replaceOrder(Order order)
    {
        if(order.buy)
        {
            Order o = new Order();
            o.price = order.oldPrice;
            o.buy = true;
            int index = Collections.binarySearch(buyOrders, o, comp);
            if(index >= 0)
            {
                for(int i = index; i < buyOrders.size(); i++)
                {
                    if(order.id.equals(buyOrders.get(i).id))
                    {
                        if(order.quantity <= order.oldQuantity && order.price == buyOrders.get(i).price)
                        {
                            buyOrders.get(i).quantity = order.quantity;
                        }
                        else
                        {
                            buyOrders.remove(i);
                            if(!tryToFill(order))
                                addOrderToBook(order);
                        }
                        return true;
                    }
                    if(order.oldPrice != buyOrders.get(i).price)
                    {
                        break;
                    }
                }
                for(int i = index - 1; i >= 0; i--)
                {
                    if(order.id.equals(buyOrders.get(i).id))
                    {
                        if(order.quantity <= order.oldQuantity && order.price == buyOrders.get(i).price)
                        {
                            buyOrders.get(i).quantity = order.quantity;
                        }
                        else
                        {
                            buyOrders.remove(i);
                            if(!tryToFill(order))
                                addOrderToBook(order);
                        } 
                        return true;
                    }
                    if(order.oldPrice != buyOrders.get(i).price)
                    {
                        break;
                    }
                }
            }
        }
        else
        {
            Order o = new Order();
            o.price = order.oldPrice;
            o.buy = false;
            int index = Collections.binarySearch(sellOrders, o, comp);
            
            if(index >= 0)
            {
                for(int i = index; i < sellOrders.size(); i++)
                {
                    if(order.id.equals(sellOrders.get(i).id))
                    {
                        if(order.quantity <= order.oldQuantity && order.price == sellOrders.get(i).price)
                        {
                            sellOrders.get(i).quantity = order.quantity;
                        }
                        else
                        {
                            sellOrders.remove(i);
                            if(!tryToFill(order))
                                addOrderToBook(order);
                        }
                        return true;
                    }
                    if(order.oldPrice != sellOrders.get(i).price)
                    {
                        break;
                    }
                }
                for(int i = index - 1; i >= 0; i--)
                {
                    if(order.id.equals(sellOrders.get(i).id))
                    {
                        if(order.quantity <= order.oldQuantity && order.price == sellOrders.get(i).price)
                        {
                            sellOrders.get(i).quantity = order.quantity;
                        }
                        else
                        {
                            sellOrders.remove(i);
                            if(!tryToFill(order))
                                addOrderToBook(order);
                        } 
                        return true;
                    }
                    if(order.oldPrice != sellOrders.get(i).price)
                    {
                        break;
                    }
                }
            }
        }
        return false;
    }
}

class Order
{
    public String type;
    public String id;
    public String symbol;
    public boolean buy;
    public int quantity;
    public double price;
    
    public int oldQuantity;
    public double oldPrice;
}