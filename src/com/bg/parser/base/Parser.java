/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bg.parser.base;

/**
 *
 * @author luis
 */
public abstract interface Parser {
    /**
     * parser: Start the parsing of the class
     */
    abstract public void parse();
    
    /**
     * getElements: Get all elements
     * @param url
     */
    abstract public void getElements(String url);
    
    /**
     * getElement: Get the information of an element.
     * @param url main page of each element
     */
    abstract public void getElement(String url);
    
    /**
     * print: Print data args with a format
     * @param msg Format of the printing
     * @param args data to be printed
     */
    abstract public void print(String msg, Object... args);
}
