/*
 * R : A Computer Language for Statistical Data Analysis
 * Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 * Copyright (C) 1997--2008  The R Development Core Team
 * Copyright (C) 2003, 2004  The R Foundation
 * Copyright (C) 2010 bedatadriven
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.renjin.pig.scripting.r;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.mapred.MiniMRCluster;
import org.apache.pig.backend.hadoop.datastorage.ConfigurationUtil;

/**
 * Copy (somewhat) of the Pig MiniCluster for making unit tests
 */
public class MiniCluster {

    private static volatile MiniCluster INSTANCE;

    private FileSystem fs = null;
    private Configuration conf = null;
    private MiniDFSCluster dfs = null;
    private MiniMRCluster cluster = null;

    public void setupCluster() {
        try {
            System.setProperty("hadoop.log.dir", "build/test/logs");
            final int dataNodes = 4;     // There will be 4 data nodes
            final int taskTrackers = 4;  // There will be 4 task tracker nodes

            // Create the configuration hadoop-site.xml file
            File conf_dir = new File("build/classes/");
            conf_dir.mkdirs();
            File conf_file = new File(conf_dir, "hadoop-site.xml");

            conf_file.delete();

            // Builds and starts the mini dfs and mapreduce clusters
            Configuration config = new Configuration();
            dfs = new MiniDFSCluster(config, dataNodes, true, null);
            fs = dfs.getFileSystem();
            cluster = new MiniMRCluster(taskTrackers, fs.getUri().toString(), 1);

            // Write the necessary config info to hadoop-site.xml
            conf = cluster.createJobConf();
            conf.setInt("mapred.submit.replication", 2);
            conf.set("dfs.datanode.address", "0.0.0.0:0");
            conf.set("dfs.datanode.http.address", "0.0.0.0:0");
            conf.set("mapred.map.max.attempts", "2");
            conf.set("mapred.reduce.max.attempts", "2");
            conf.set("pig.jobcontrol.sleep", "100");
            conf.writeXml(new FileOutputStream(conf_file));

            // Set the system properties needed by Pig
            System.setProperty("cluster", conf.get("mapred.job.tracker"));
            System.setProperty("namenode", conf.get("fs.default.name"));
            System.setProperty("junit.hadoop.conf", conf_dir.getPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Properties getProperties() {
        return ConfigurationUtil.toProperties(conf);
    }

    public Configuration getConfig() {
        return conf;
    }

    public void shutdown() {
        if (cluster != null) { cluster.shutdown(); }
            cluster = null;
    }

    public synchronized static MiniCluster getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MiniCluster();
            INSTANCE.setupCluster();
        }

        return INSTANCE;
    }
}
