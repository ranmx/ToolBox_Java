package com.fosun;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import org.apache.spark.sql.catalyst.ScalaReflection.Schema;
import org.apache.spark.sql.types.StructType;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import org.apache.spark.sql.Dataset;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Row;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.Encoders;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import com.fosun.tableObjects.*;

import scala.Function1;

/**
 * This is a sample class to launch a rule.
 */
public class DroolsTest {

	public static Iterator<Row> ruleExecutor(Iterator<Row> iter) {
	    	return iter;
	    }

	public static final void main(String[] args) {
        try {
        	// spark session
        	SparkConf conf = new SparkConf();
        	
        	SparkSession spark = SparkSession
        			.builder()
        			.master("local[2]")
        			.appName("Drool Demo")
        			.config(conf)
        			.getOrCreate();
        	
        	// configuration file
//        	Config config = ConfigFactory.load("app.conf");
//        	String path = config.getString("parquetPath");
        	
        	// get file
			JavaRDD<Row> parFile = spark.read().parquet("./src/main/resources/yongan.snappy.parquet").javaRDD();
			
            // go !         	
        	JavaRDD<CleanedPerson> resultRDD = parFile.mapPartitions(iterator->{
                // load up the knowledge base
    	        KieServices ks = KieServices.Factory.get();
        	    KieContainer kContainer = ks.getKieClasspathContainer();
            	KieSession kSession = kContainer.newKieSession("ksession-rules");
            	
        		List<CleanedPerson> persons = new ArrayList<>();
        		List<Row> rows = new ArrayList<Row>();
        		while (iterator.hasNext()) {
        			Row row = iterator.next();
        			YongAn rawData = new YongAn(row);
        			kSession.insert(rawData);
    				persons.add(rawData.cleanedPerson);
        		}
        		kSession.startProcess("RF1", null);
        		kSession.fireAllRules();
        		kSession.dispose();
        		return persons.iterator();
				}
        	);



			Dataset<CleanedPerson> cleanedPerDF = spark.createDataset(resultRDD.rdd(), Encoders.bean(CleanedPerson.class));
			Dataset<Row> cleanedPerDF2 = spark.createDataFrame(resultRDD, CleanedPerson.class);
			System.out.println("------------------------------------------------");
			cleanedPerDF.show();
			System.out.println("------------------------------------------------");
			cleanedPerDF2.show();
			System.out.println("------------------------------------------------");
			StructType mySchema = Encoders.bean(CleanedPerson.class).schema();
			System.out.print(mySchema);
        	resultRDD.collect().forEach(s -> System.out.println(s.toString()));


        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
   
    
}
