/*
 * Copyright (C) 2010 Minwoo Jeong (minwoo.j@gmail.com).
 * This file is part of the "bitextOpenIE" distribution.
 * http://github.com/minwoo/bitextOpenIE/
 * This software is provided under the terms of LGPL.
 */

package openie.extractor;

import openie.crf.CRF;
import openie.text.*;
import openie.util.*;

import org.apache.log4j.Logger;
import jargs.gnu.CmdLineParser;

public class Test {

    public static void main(String[] args) {

        Logger logger = Logger.getLogger(Test.class);

        // command line parsing
        CmdLineParser cmdParser = new CmdLineParser();
        CmdLineParser.Option debug = cmdParser.addBooleanOption('d', "debug");
        CmdLineParser.Option verbose = cmdParser.addBooleanOption('v', "verbose");
        CmdLineParser.Option configfile = cmdParser.addStringOption('c', "config");

        try {
           cmdParser.parse(args);
        }
        catch (CmdLineParser.OptionException e) {
            logger.error(e.getMessage());
            logger.error("Usage: java -cp ${CLASSPATH} opeie.extractor.Test " +
                    "[-c,--config] config_file [{-v,--verbose}] [{-d,--debug}]");
            System.exit(2);
        }

        String configFileName = (String)cmdParser.getOptionValue(configfile);
        Boolean isDebug = (Boolean)cmdParser.getOptionValue(debug, Boolean.TRUE);
        Boolean isVerbose = (Boolean)cmdParser.getOptionValue(verbose, Boolean.TRUE);

        // running
        try {
            Configure config = new Configure(configFileName);
            String classifierName = config.contains("class") ? config.getString("class") : "openie.crf.MaxEnt";
            
            // classifier
            CRF classifier = (CRF) Class.forName(classifierName).getConstructor(new Class[]{}).newInstance(new Object[]{});
            logger.info("===" + classifier.getClass().getName() + "===");
            
            // training mode
            if (config.contains("train_file")) {
            	classifier.setParam(new Parameter());
                UnitextCorpus trainSet = new UnitextCorpus(classifier.getParam());
            	trainSet.readFile(config.getString("train_file"), true);
            	if (config.contains("train") && config.getBoolean("train")) 
            		classifier.train(trainSet, config);
            	// model save
            	if (config.contains("model_file")) 
            		classifier.save(config.getString("model_file"));
            }
            // test mode
            if (config.contains("test_file")) {
            	// model load
            	classifier.setParam(new Parameter());
            	if (config.contains("model_file")) 
            		classifier.load(config.getString("model_file"));
                UnitextCorpus testSet = new UnitextCorpus(classifier.getParam());
            	testSet.readFile(config.getString("test_file"), false);
            	if (config.contains("test") && config.getBoolean("test")) 
            		classifier.test(testSet, config);
            }
            
        }
        catch (Exception e) {
        	logger.error("error " + e.getMessage());
        	e.printStackTrace();
            System.exit(2);
        }

    }

}
