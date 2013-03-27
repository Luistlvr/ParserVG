/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bg.parser.html;

import com.bg.parser.db.MySql;
import com.bg.parser.html.helper.NaturaGlobals;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

/**
 *
 * @author luis
 */
public class NaturaParser {
    /*!< URL of the main page */
    private String url_root = "http://scf.natura.net/Produtos/Default.aspx";
    
    /*!< Database Connection */
    private MySql database;
    
    private int actualCategory;
    
    public NaturaParser() {
        database = MySql.getConnection();
    }
    
    /**
     * parse: Get all the categories, and call the function to get the products
     * also.
     */
    public void parse() {
        try {
            Document document  = Jsoup.connect(url_root).get();
            Elements list_menu = document.select("ul[id$=" + NaturaGlobals.LIST_ID + "]");
            
            List<Node> items = list_menu.first().childNodes();
            for(Node item : items) {
                List<Node> categories = item.childNodes();
                for(Node category : categories) {
                    actualCategory = database.executeStatement("INSERT INTO Categories (Name, Description) VALUES ('" + 
                            category.childNode(0).attr("text") + "','" + category.attr("title") + "');");
                    
                    print("Titulo: %s\nDescripcion: %s\nProducts:", 
                            category.childNode(0).attr("text"), 
                            category.attr("title"));
                    getProducts(category.attr(NaturaGlobals.ABS_LINK));
                }
                System.out.println("\n");
            }
        } catch (SQLException | IOException ex) {
            Logger.getLogger(NaturaParser.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    /**
     * getProducts: Get all products.
     * @param url_child main page of each category
     */
    private void getProducts(String url_galery) throws IOException {
        Document document  = Jsoup.connect(url_galery).timeout(NaturaGlobals.TIMEOUT).get();
        Elements list_menu = document.select("a[id$=" + NaturaGlobals.LINK_ID + "]");
            
        if(list_menu.attr(NaturaGlobals.ABS_LINK).equals("")) {
            System.out.println("No Link");
            return;
        }
            
        Document products  = Jsoup.connect(list_menu.attr("abs:href")).timeout(NaturaGlobals.TIMEOUT).get();
        Elements galery    = products.select("dl[class$=" + NaturaGlobals.DL_CLASS + "]");
            
        for(Element item : galery) {
            getProduct(item.child(0).child(0).attr(NaturaGlobals.ABS_LINK));
        }
    }
    
    /**
     * getProduct: Get the information of a product.
     * @param url_product main page of each product
     */
    private void getProduct(String url_product) throws IOException {
        Connection.Response response = Jsoup.connect(url_product).ignoreHttpErrors(true).timeout(NaturaGlobals.TIMEOUT).execute();
        if(response.statusCode() == NaturaGlobals.ERROR_NOT_FOUND) {
            System.out.println("Page Not Found");
            return;
        }
        else if(response.statusCode() == NaturaGlobals.ERROR_EXCEPTION) {
            System.out.println("Unhandled Exception");
            return;
        }
        
        Document document  = Jsoup.connect(url_product).timeout(NaturaGlobals.TIMEOUT).get();     
        try {
            Elements images = document.select("img[alt$=produto vertical]");
        
            database.executeStatement("INSERT INTO Products (Name, Description, Indication, Benefits, Content,"
                    + "HowToUse, Ingredients, Price, Image,CategoryId) VALUES ('" 
                    + document.getElementsByTag("title").text().substring(18) + "','" 
                    + document.getElementsByTag("meta").get(5).attr("content") + "','"
                    + document.select("span[id$=" + NaturaGlobals.P_INDICATION + "]").text() + "','"
                    + document.select("span[id$=" + NaturaGlobals.P_BENEFITS + "]").text() + "','"
                    + document.select("span[id$=" + NaturaGlobals.P_CONTENT + "]").text() + "','"
                    + document.select("span[id$=" + NaturaGlobals.P_HOW_TO_USE + "]").text() + "','"
                    + document.select("span[id$=" + NaturaGlobals.P_INGREDIENTS + "]").text() + "','"
                    + document.select("td[id$=" + NaturaGlobals.P_PRICE + "]").text() + "','"
                    + images.first().attr("abs:src") + "'," + actualCategory + ");");
        } catch (SQLException ex) {
            Logger.getLogger(NaturaParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        print("\t* Product: %s\n\t  Description: %s\n\n\t  --Characteristics\n\t"
                + "  Principle Active: %s\n\t  Indication: %s\n\t  Benefits: %s\n\t"
                + "  Content: %s\n\n\t  --Way To Use\n\t  %s\n\n\t  --Ingredients & Actives\n\t"
                + "  Ingredients: %s\n\t  Actives: %s\n\n\t  --Environmental Table\n\t"
                + "  Ingredients: %s\n\t  Packing: %s\n\n\t  Price: %s\n\t  Refil: %s\n", 
                document.getElementsByTag("title").text().substring(18),
                document.getElementsByTag("meta").get(5).attr("content"),
                document.select("span[id$=" + NaturaGlobals.P_ACTIVE + "]").text(),
                document.select("span[id$=" + NaturaGlobals.P_INDICATION + "]").text(),
                document.select("span[id$=" + NaturaGlobals.P_BENEFITS + "]").text(),
                document.select("span[id$=" + NaturaGlobals.P_CONTENT + "]").text(),
                document.select("span[id$=" + NaturaGlobals.P_HOW_TO_USE + "]").text(),
                document.select("span[id$=" + NaturaGlobals.P_INGREDIENTS + "]").text(),
                document.select("span[id$=" + NaturaGlobals.P_ACTIVES + "]").text(),
                document.select("span[id$=" + NaturaGlobals.P_PRODUCTS + "]").text(),
                document.select("span[id$=" + NaturaGlobals.P_PACKING + "]").text(),
                document.select("td[id$=" + NaturaGlobals.P_PRICE + "]").text(),
                document.select("td[id$=" + NaturaGlobals.P_REFIL + "]").text());
        
        System.out.println("\t  --Images:");
        Elements images = document.select("img[alt$=produto vertical]");
        for(Element image : images) {
            print("\t  %s", image.attr("abs:src"));
        }
        System.out.println("\n");
    }
    
    private static void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        NaturaParser temp = new NaturaParser();
        temp.parse();
    }
}
