/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bg.parser.helper;

import com.bg.parser.db.MySql;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author luis
 */
public class NaturaUtils {
    /*!< Frecuency of words */
    private double[][] vector;
    private static int rows;
    private static int columns;
    
    /*!< Url of the stopwords document */
    private InputStream url;
    
    /*!< Stopwords */
    private HashSet stopwords;
    
    /*!< All words in documents without repetitions */
    private SortedSet vocabulary;
    
    /*!< Database Connection */
    private MySql database;
    
    /*!< Test Data */
    private String product = "Água de colônia:Fragrância com notas cítricas e "
            + "florais, com fundo sândalo vanila musc. Possui embalagem "
            + "inquebrável especialmente desenvolvida para proporcionar maior "
            + "segurança à mãe e ao bebê. Dermatologicamente testado. Ph "
            + "fisiológico.Toalha:Toalha de banho infantil com capuz na cor "
            + "branca e viés na cor verde, tecido 100% algodão e logo de Mamãe e "
            + "Bebê bordado no capuz.";
    
    private String comment1 = "Se alguém se interessar no Chronos ou em outros "
            + "produto da Natura me procurem pelo face ou me liguem, vou adorar "
            + "ser sua condultora!!!";
    
    private String comment2 = "Eu que o diga!!!!!! Produtos M A R A V I L H O S "
            + "O S obrigada NATURA por me deixar assim tão feliz obrigada tbm à "
            + "minha consultora.";
    
    
    /**
     * NaturaUtils: Constructor to initialize the global variables
     */
    public NaturaUtils() {
        url = NaturaUtils.class.getResourceAsStream(NaturaGlobals.URL_STOPWORDS);
        stopwords  = new HashSet<>();
        vocabulary = new TreeSet<>();
        database = MySql.getConnection();
    }
    
    /**
     * loadStopwords: Load all the stopwords from the document
     */
    private void loadStopwords() {
        String line;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(url));
            while((line = br.readLine()) != null) {
                stopwords.add(line);
            }
        } catch (IOException ex) {
            Logger.getLogger(NaturaUtils.class.getName()).log(Level.SEVERE, null, ex);  
        } 
    }
    
    /**
     * removeStopwords: Removes the stopwords from a test
     * @param text words of a document
     * @return String[] words of a document without the stopwords
     */
    private String[] removeStopwords(String[] text) {
        List<String> textWithoutStopwords = new ArrayList<>();  
        
        for(int i = 0; i < text.length; ++i) {
            if(!isStopword(cleanWords(text[i])))
                textWithoutStopwords.add(text[i]);
        }
        return textWithoutStopwords.toArray(new String[textWithoutStopwords.size()]);
    }
    
    /**
     * isStopword: Check if a word is stopword
     * @param word a word
     * @return boolean true if is stopword otherwise false
     */
    private boolean isStopword(String word) {
        return stopwords.contains(word);
    }
    
    /**
     * createVocabulary: Add words to the vocabulary, only if the words are not
     * already included already
     * @param words words of a document
     */
    private void createVocabulary(String[] words) {
        for(int i = 0; i < words.length; ++i)
            vocabulary.add(words[i]);
    }
    
    private void printVocabulary() {
        for(Object word : vocabulary) {
            System.out.print(word + " ");
        }
        System.out.println("\n");
    }
    
    private String cleanWords(String word) {
        String temp = ""; 
        
        for(int i = 0; i < word.length(); i++) {
            if(word.charAt(i) == '(' || word.charAt(i) == ')' || word.charAt(i) == '[' ||
               word.charAt(i) == ']' || word.charAt(i) == '{' || word.charAt(i) == '}' ||
               word.charAt(i) == ',' || word.charAt(i) == '.' || word.charAt(i) == ';' ||
               word.charAt(i) == ':' || word.charAt(i) == '?' || word.charAt(i) == '¿' ||
               word.charAt(i) == '¡' || word.charAt(i) == '!' || word.charAt(i) == '"' ||
               word.charAt(i) == '-' || word.charAt(i) == '_' || word.charAt(i) == '>' ||
               word.charAt(i) == '<' || word.charAt(i) == '/' || word.charAt(i) == '$' ||
               word.charAt(i) == '%' || word.charAt(i) == '@' || word.charAt(i) == '#');
            else
               temp += word.charAt(i);            
        }
        return temp;
    }
    
    /**
     * countWords: Count words of the vocabulary that exists on a document and 
     * add the amount to the vector
     * @param documents All the documents
     */
    private void countWords(List<String[]> documents) {
        for(int i = 0; i < documents.size(); ++i) {
            List<String>  words = Arrays.asList(documents.get(i));
            int pos = 0;
            
            for(Object word : vocabulary) {
                vector[i][pos] = Collections.frequency(words, word);
                pos++;
            }
        }
    }
       
    /**
     * normalize: Normalize the vector with the highest value of frecuency 
     * @param i The amount of rows
     * @param j The amount of columns
     */
    private void normalize() {
        double high = highValue();
        
        for(int w = 0; w < rows; w++) 
            for(int p = 0; p < columns; p++)
                vector[w][p] /= high;
    }
    
    /**
     * highValue: Gets the highValue of the vector
     * @param i The amount of rows
     * @param j The amount of columns
     * @return double The highest value of the vector
     */
    private double highValue() {
        double max = 0;
        for(int w = 0; w < rows; w++) {
            for(int p = 0; p < columns; p++){
                if(max < vector[w][p])
                    max = vector[w][p];
            }
        }
        return max;
    }
    
    /**
     * distance: Gets the distances between the documents values
     * @return double[] the distances between the first row of vector and the 
     * others
     */
    private double[] distance() {
        double[] max = new double[rows - 1];
       
        for(int w = 1; w < rows; w++) {
            double distance = 0.0;
            for(int p = 0; p < columns; p++) {
                distance += Math.pow(vector[0][p] - vector[w][p], 2.0);
            }
            max[w - 1] = Math.sqrt(distance);
        }
        return max;
    }
    
    /*
     * print: Print the vector
     */
    private void print() {
        for(int w = 0; w < rows; w++) {
            System.out.println("");
            for(int p = 0; p < columns; p++){
                System.out.print(vector[w][p] + " ");
            }
        }
        System.out.println("\n");
    }
    
    /**
     * compare: Compare a text with others
     * @param text Text to be compared
     * @param textsToCompare Texts to compare with text
     */
    private void compare(List<String> textsToCompare) {
        loadStopwords();
        List<String[]> temp = new ArrayList();

        for(String textToCompare : textsToCompare) {
            String[] wordsTextToCompare = textToCompare.split(" ");
            wordsTextToCompare = removeStopwords(wordsTextToCompare);
            createVocabulary(wordsTextToCompare);
            temp.add(wordsTextToCompare);
        }
        rows    = textsToCompare.size();
        columns = vocabulary.size();
        vector = new double[rows][columns];
        countWords(temp);
        normalize();
        double[] dist = distance();
        double min = Integer.MAX_VALUE;
        int num = 0;
        
        for(int i = 0; i < dist.length; ++i) {
            if(min > dist[i]) {
                min = dist[i];
                num = i;
            }
        }
        System.out.println(num + " (" + min + ")");
    }
    
    private void clear() {
        stopwords.clear();
        vocabulary.clear();
    }
    
    /**
     * start: Starts an example, adding a product and documents to compare
     */
    public void start() {
        List<String> textToCompare = new ArrayList();
        textToCompare.add("free");
        int productId = 0;
        try {
            ResultSet feeds = database.executeQuery("SELECT Message FROM Feeds");
            while(feeds.next()) {
                textToCompare.add(feeds.getString("Message"));
            }
            
            ResultSet result = database.executeQuery("SELECT Description FROM Products");
            while(result.next()) {
                System.out.print(productId + " <- Similar -> ");
                textToCompare.set(0, result.getString("Description"));
                clear();
                compare(textToCompare);
                productId++;
            }
        } catch (SQLException ex) {
            Logger.getLogger(NaturaUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
