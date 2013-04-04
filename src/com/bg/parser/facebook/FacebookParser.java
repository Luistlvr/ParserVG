/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bg.parser.facebook;

import com.bg.parser.base.Parser;
import com.bg.parser.db.MySql;
import com.bg.parser.helper.NaturaGlobals;
import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.DefaultJsonMapper;
import com.restfb.DefaultJsonMapper.JsonMappingErrorHandler;
import com.restfb.DefaultWebRequestor;
import com.restfb.FacebookClient;
import com.restfb.FacebookClient.AccessToken;
import com.restfb.Parameter;
import com.restfb.json.JsonArray;
import com.restfb.json.JsonObject;
import com.restfb.types.Post;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 *
 * @author luis
 */
public class FacebookParser implements Parser {
    FacebookClient facebookClient;
    
    /*!< Database Connection */
    private MySql database;
    
    private int actualFeed;
    private long actualLikes;
    private long actualComments;
    
    public FacebookParser() {
        database = MySql.getConnection();
        AccessToken accessToken = new DefaultFacebookClient().obtainAppAccessToken(NaturaGlobals.fbAppId, NaturaGlobals.fbSecretKey);

        facebookClient = new DefaultFacebookClient(accessToken.getAccessToken(),
            new DefaultWebRequestor(), new DefaultJsonMapper(new JsonMappingErrorHandler() {
                @Override
                public boolean handleMappingError(String unmappableJson, Class<?> targetType, Exception e) {
                    System.err.println(String.format("Uh oh, mapping %s to %s failed...", unmappableJson, targetType));
                    return true;
                }
            }));
    }
    
    public void getElementDaysAgo(long days) {
        Date oneWeekAgo = new Date(System.currentTimeMillis() - 1000L * 60L * 60L * 24L * days);

        Connection<Post> filteredFeed = facebookClient.fetchConnection(NaturaGlobals.fbPageName + "/feed", Post.class,
                Parameter.with("limit", NaturaGlobals.AMOUNT_OF_FILTER), Parameter.with("until", "yesterday"),
                Parameter.with("since", oneWeekAgo));

        System.out.println("Filtered feed count: " + filteredFeed.getData().size());
    }
    
    @Override
    public void parse() {
        JsonObject facebookPage = facebookClient.fetchObject(NaturaGlobals.fbPageName, JsonObject.class);
        /*print("Website: %s\nAbout: %s\nFounded: %s\nCategory: %s\nLikes: %d", 
                facebookPage.getString("website"), facebookPage.getString("about"),
                facebookPage.getString("founded"), facebookPage.getString("category"),
                facebookPage.getLong("likes"));*/
        
        getElements(NaturaGlobals.fbPageName);
    }

    @Override
    public void getElements(String url) {
        JsonObject feedsConnection = facebookClient.fetchObject(url + "/feed", JsonObject.class, 
                Parameter.with("limit", NaturaGlobals.AMOUNT_OF_FEEDS));
        JsonArray feeds = feedsConnection.getJsonArray("data");
        //print("\nFeeds:");
        
        for(int i = 0; i < feeds.length(); ++i) {
            JsonObject feed = feeds.getJsonObject(i);
            
            if(feed.has("id") && feed.has("likes") && feed.has("comments") && feed.has("from") && 
                    feed.has("message") && feed.has("shares")) {
                long likes = feed.getJsonObject("likes").getLong("count"), 
                     comments = feed.getJsonObject("comments").getLong("count");
                String feedId = feed.getString("id");
                /*print("Id: %s\nCategory: %s\nShares: %d\nLikes: %d\nComments: %d\nMessage: %s", 
                        feedId, feed.getJsonObject("from").getString("category"),
                        feed.getJsonObject("shares").getLong("count"), likes,
                        comments, feed.getString("message"));*/
                
                if(likes > 0 || comments > 0) {
                    System.out.println("INSERT INTO Feeds (Id, Message, Category, Likes, Shares, Comments) VALUES ('" +
                                feedId + "','" + StringEscapeUtils.escapeXml(feed.getString("message")) + "','" + feed.getJsonObject("from").getString("category") + "'," + 
                                likes + "," + feed.getJsonObject("shares").getLong("count") + "," + comments + ");");
                    try {                        
                        actualFeed = database.executeStatement("INSERT INTO Feeds (Id, Message, Category, Likes, Shares, Comments) VALUES ('" +
                                feedId + "','" + StringEscapeUtils.escapeXml(feed.getString("message")) + "','" + feed.getJsonObject("from").getString("category") + "'," + 
                                likes + "," + feed.getJsonObject("shares").getLong("count") + "," + comments + ");");
                        actualLikes = likes;
                        actualComments = comments;
                    } catch (SQLException ex) {
                        Logger.getLogger(FacebookParser.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    getElement(feedId);
                }
            }
        }
    }

    @Override
    public void getElement(String url) {
        /*JsonObject commentsConnection = facebookClient.fetchObject(url + "/comments", JsonObject.class, 
                Parameter.with("limit", NaturaGlobals.AMOUNT_OF_COMMENTS));*/
        JsonObject commentsConnection = facebookClient.fetchObject(url + "/comments", JsonObject.class, 
                Parameter.with("limit", actualComments));
        JsonArray comments = commentsConnection.getJsonArray("data");
        //print("\nComments:");
        for(int i = 0; i < comments.length(); ++i) {
            JsonObject comment = comments.getJsonObject(i);
            
            if(comment.has("id") && comment.has("from") && comment.has("like_count") && comment.has("message")) {
                /*print("\tId: %s\n\tName: %s\n\tLikes: %d\n\tMessage: %s\n", 
                        comment.getString("id"), comment.getJsonObject("from").getString("name"),
                        comment.getLong("like_count"), comment.getString("message"));*/
                System.out.println("INSERT INTO Comments (Id, Name, Message, Likes, idFeed) VALUES ('" +
                            comment.getString("id") + "','" + StringEscapeUtils.escapeXml(comment.getJsonObject("from").getString("name")) + "','" +
                            StringEscapeUtils.escapeXml(comment.getString("message")) + "'," + comment.getLong("like_count") + "," + actualFeed + ");");
                try {
                    database.executeStatement("INSERT INTO Comments (Id, Name, Message, Likes, idFeed) VALUES ('" +
                            comment.getString("id") + "','" + StringEscapeUtils.escapeXml(comment.getJsonObject("from").getString("name")) + "','" +
                            StringEscapeUtils.escapeXml(comment.getString("message")) + "'," + comment.getLong("like_count") + "," + actualFeed + ");");
                } catch (SQLException ex) {
                    Logger.getLogger(FacebookParser.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        /*JsonObject likesConnection = facebookClient.fetchObject(url + "/likes", JsonObject.class, 
                Parameter.with("limit", NaturaGlobals.AMOUNT_OF_LIKES));*/
        JsonObject likesConnection = facebookClient.fetchObject(url + "/likes", JsonObject.class, 
                Parameter.with("limit", actualLikes));
        JsonArray likes = likesConnection.getJsonArray("data");
        //print("Likes:");
        for(int i = 0; i < likes.length(); ++i) {
            JsonObject like = likes.getJsonObject(i);
            
            if(like.has("id") && like.has("name")) {
                /*print("\tId: %s\n\tName: %s\n", like.getString("id"), 
                        like.getString("name"));*/
                System.out.println("INSERT INTO Likes (Id, Name, idFeed) VALUES ('" +
                            like.getString("id") + "','" + StringEscapeUtils.escapeXml(like.getString("name")) + 
                            "'," + actualFeed + ");");
                try {
                    database.executeStatement("INSERT INTO Likes (Id, Name, idFeed) VALUES ('" +
                            like.getString("id") + "','" + StringEscapeUtils.escapeXml(like.getString("name")) + 
                            "'," + actualFeed + ");");
                } catch (SQLException ex) {
                    Logger.getLogger(FacebookParser.class.getName()).log(Level.SEVERE, null, ex);
                }
            }      
        }
    }

    @Override
    public void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }
}
