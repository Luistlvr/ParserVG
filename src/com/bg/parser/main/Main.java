/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bg.parser.main;

import com.bg.parser.facebook.FacebookParser;

/**
 *
 * @author luis
 */
public class Main {
        /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        FacebookParser test = new FacebookParser();
        test.parse();
    }
}
