/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bg.parser.facebook;

import com.bg.parser.base.Parser;
import com.bg.parser.html.helper.NaturaGlobals;
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
import java.util.Date;

/**
 *
 * @author luis
 */
public class FacebookParser implements Parser {
    FacebookClient facebookClient;
    
    public FacebookParser() {
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
        print("Website: %s\nAbout: %s\nFounded: %s\nCategory: %s\nLikes: %d", 
                facebookPage.getString("website"), facebookPage.getString("about"),
                facebookPage.getString("founded"), facebookPage.getString("category"),
                facebookPage.getLong("likes"));
        
        getElements(NaturaGlobals.fbPageName);
    }

    @Override
    public void getElements(String url) {
        JsonObject feedsConnection = facebookClient.fetchObject(url + "/feed", JsonObject.class, 
                Parameter.with("limit", NaturaGlobals.AMOUNT_OF_FEEDS));
        JsonArray feeds = feedsConnection.getJsonArray("data");
        print("\nFeeds:");
        
        for(int i = 0; i < feeds.length(); ++i) {
            JsonObject feed = feeds.getJsonObject(i);
            
            if(feed.has("id") && feed.has("likes") && feed.has("comments")) {
                long likes = feed.getJsonObject("likes").getLong("count"), 
                     comments = feed.getJsonObject("comments").getLong("count");
                String feedId = feed.getString("id");
                print("Id: %s", feedId);
                
                if(feed.has("from"))
                    print("Category: %s", feed.getJsonObject("from").getString("category"));
                if(feed.has("shares"))
                    print("Shares: %d", feed.getJsonObject("shares").getLong("count"));
                
                print("Likes: %d", likes);
                print("Comments: %d", comments);
                
                if(feed.has("message"))
                    print("Message: %s", feed.getString("message"));
                
                if(likes > 0 || comments > 0)
                    getElement(feedId);
            }
        }
    }

    @Override
    public void getElement(String url) {
        JsonObject commentsConnection = facebookClient.fetchObject(url + "/comments", JsonObject.class, 
                Parameter.with("limit", NaturaGlobals.AMOUNT_OF_COMMENTS));
        JsonArray comments = commentsConnection.getJsonArray("data");
        print("\nComments:");
        for(int i = 0; i < comments.length(); ++i) {
            JsonObject comment = comments.getJsonObject(i);
            
            if(comment.has("id")) {
                print("\tId: %s", comment.getString("id"));
                
                if(comment.has("from"))
                    print("\tName: %s", comment.getJsonObject("from").getString("name"));
                if(comment.has("like_count")) 
                    print("\tLikes: %d", comment.getLong("like_count"));
                if(comment.has("message"))
                    print("\tMessage: %s\n", comment.getString("message"));
            }
        }
        
        JsonObject likesConnection = facebookClient.fetchObject(url + "/likes", JsonObject.class, 
                Parameter.with("limit", NaturaGlobals.AMOUNT_OF_LIKES));
        JsonArray likes = likesConnection.getJsonArray("data");
        print("Likes:");
        for(int i = 0; i < likes.length(); ++i) {
            JsonObject like = likes.getJsonObject(i);
            
            if(like.has("id") && like.has("name"))
                print("\tId: %s\n\tName: %s\n", like.getString("id"), 
                        like.getString("name"));
        }
    }

    @Override
    public void print(String msg, Object... args) {
        System.out.println(String.format(msg, args));
    }
}
