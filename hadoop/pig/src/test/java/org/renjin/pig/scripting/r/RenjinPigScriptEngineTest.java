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
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import junit.framework.TestCase;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.pig.ExecType;
import org.apache.pig.PigServer;
import org.apache.pig.backend.hadoop.datastorage.ConfigurationUtil;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.PigContext;
import org.apache.pig.scripting.ScriptEngine;
import org.apache.pig.tools.pigstats.PigStats;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.base.Charsets.UTF_8;
import static java.lang.Math.sqrt;

public class RenjinPigScriptEngineTest extends TestCase {

    private PigServer pigServer;
    private static MiniCluster cluster = MiniCluster.getInstance();

    @Before
    public void setUp() throws Exception {
        pigServer = new PigServer(ExecType.MAPREDUCE, cluster.getProperties());
    }

    @Test
    public void testRScript01() throws Exception {
        String[] script = {
          //"library(pig)",
          "firstNoneEmpty <- function(...) {",
            "for (i in 1:length(...)) {",
              "if (length(...[i]) > 1) {",
                "return ...[i]",
              "}",
            "}",
          "}",

          "main <- function() {",
            "}"
        };

        String[] input = {
            "1\t3",
            "2\t4",
            "3\t5"
        };

        deleteFile(pigServer.getPigContext(), "simple_table");
        deleteFile(pigServer.getPigContext(), "simple_out");
        createInputFile(pigServer.getPigContext(), "simple_table", input);
        createLocalInputFile("testScript.py", script);

        //ScriptEngine engine = ScriptEngine.getInstance("renjin");
        ScriptEngine engine = ScriptEngine.getInstance("org.renjin.pig.scripting.r.RenjinPigScriptEngine");
        Map<String, List<PigStats>> stats = engine.run(pigServer.getPigContext(), "simple_out");
        assertEquals(1, stats.size());
        Iterator<List<PigStats>> it = stats.values().iterator();
        assertTrue(it.next().get(0).isSuccessful());

        //String[] output = Util.readOutput(pigServer.getPigContext(), "simple_out");

        //assertEquals(3, output.length);
        //assertEquals(output[0], "1\t1\t3");
        //assertEquals(output[1], "2\t2\t4");
        //assertEquals(output[2], "3\t3\t5");
    }

    @Test
    public void testRFunction01() throws Exception{
        String[] script = {
            "test <- function() { c('test','test2') }",
            // We should make an R library called pig ...
            "attr(test, 'pig.outputSchema') <- 'word:chararray'",
            "test2 <- function(x) sqrt(x)",
            "attr(test2, 'pig.outputSchema') <- 'val:int'",
            "test3 <- function(x,y) x * y",
            "attr(test3, 'pig.outputSchema') <- 'val:int'",
            "test4 <- function(...) ...",
            "attr(test4, 'pig.outputSchema') <- 'val:bytearray'",
            "my_prim <- `+`",
            "attr(my_prim, 'pig.outputSchema') <- 'val:int'",
        };

        String[] input = {
            "one\t1",
            "two\t2",
            "three\t3"
        };

        createInputFile(cluster.getConfig(), "table_testRFunction01Script", input);
        createLocalInputFile("testRFunction01Script.r", script);

        // Test the namespace
        //pigServer.registerCode("testRFunction01Script.r", "renjin", "testfns");
        pigServer.registerCode("testRFunction01Script.r",
            "org.renjin.pig.scripting.r.RenjinPigScriptEngine", "testfns");
        pigServer.registerQuery("A = LOAD 'table_testRFunction01Script' as (a0:chararray, a1:long);");
        pigServer.registerQuery("B = foreach A generate testfns.test();");
        pigServer.registerQuery("C = foreach A generate testfns.test2($1);");
        pigServer.registerQuery("D = foreach A generate testfns.test3(2, $1);");
        pigServer.registerQuery("E = foreach A generate testfns.test4($1);");
        pigServer.registerQuery("F = foreach A generate testfns.my_prim(2, $1);");

        assertIterator(pigServer.openIterator("B"), "\"test\"", "\"test\"", "\"test\"");
        assertIterator(pigServer.openIterator("C"), sqrt(1), sqrt(2), sqrt(3));
        assertIterator(pigServer.openIterator("D"), 2, 4, 6);
        assertIterator(pigServer.openIterator("E"), 1, 2, 3);
        assertIterator(pigServer.openIterator("F"), 3, 4, 5);
    }

    // Helper functions follow
    //------------------------------------------------------------------------------------------
    public static void assertIterator(Iterator<Tuple> tupleIterator, Object... expected) throws Exception {
        for (Object o : expected) {
            assertTrue(tupleIterator.hasNext());
            Tuple t = tupleIterator.next();
            assertEquals(t.get(0), o);
        }
        assertFalse(tupleIterator.hasNext());
    }

    private static void createLocalInputFile(String fileName, String[] inputData) throws IOException {
        File f = new File(fileName);
        f.deleteOnExit();
        Files.write(Joiner.on('\n').join(inputData).getBytes(UTF_8), f);
    }

    public static void deleteFile(PigContext pigContext, String fileName)  throws IOException {
        Configuration conf = ConfigurationUtil.toConfiguration(pigContext.getProperties());
        FileSystem fs = FileSystem.get(conf);
        fs.delete(new Path(fileName), true);
    }

    public static void createInputFile(PigContext pigContext, String fileName, String[] input) throws IOException {
        Configuration conf = ConfigurationUtil.toConfiguration(pigContext.getProperties());
        createInputFile(conf, fileName, input);
    }

    public static void createInputFile(Configuration conf, String fileName, String[] input) throws IOException {
        FileSystem fs = FileSystem.get(conf);

        if(fs.exists(new Path(fileName))) {
            throw new IOException("File " + fileName + " already exists on the FileSystem");
        }

        FSDataOutputStream stream = fs.create(new Path(fileName));
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(stream, "UTF-8"));
        for (final String anInputData : input) {
            pw.println(anInputData);
        }
        pw.close();
    }

}
