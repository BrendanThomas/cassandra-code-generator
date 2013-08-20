import static com.netflix.astyanax.examples.ModelConstants.*;

import com.netflix.astyanax.ddl.ColumnDefinition;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;
import com.netflix.astyanax.ddl.FieldMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.CqlResult;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.serializers.IntegerSerializer;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.nio.ByteBuffer;

public class SchemaFileGenerator {
    private static final Logger logger = LoggerFactory.getLogger(SchemaFileGenerator.class);

    private AstyanaxContext<Keyspace> context;
    private Keyspace keyspace;
    public String outDirectory;

    public SchemaFileGenerator(String outDirectory)
    {
         this.outDirectory = outDirectory;

    }


    public void init() {
        logger.debug("init()");

        context = new AstyanaxContext.Builder()
                .forCluster("Test Cluster")
                .forKeyspace("")//Keyspace Name
                .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
                        .setDiscoveryType(NodeDiscoveryType.RING_DESCRIBE)
                )
                .withConnectionPoolConfiguration(new ConnectionPoolConfigurationImpl("MyConnectionPool")
                        .setPort(9160)
                        .setMaxConnsPerHost(1)
                        .setSeeds("localhost:9160")
                )
                .withAstyanaxConfiguration(new AstyanaxConfigurationImpl()
                        .setCqlVersion("3.0.2")
                        .setTargetCassandraVersion("1.2.5"))
                .withConnectionPoolMonitor(new CountingConnectionPoolMonitor())
                .buildKeyspace(ThriftFamilyFactory.getInstance());

        context.start();
        keyspace = context.getEntity();
    }

    public void generateCode()   {
        logger.debug("generateCode()");
        try {

            List<ColumnFamilyDefinition> columnFamilyDefinitionList = keyspace.describeKeyspace().getColumnFamilyList();
            ColumnFamilyDefinition columnFamilyDefinition;
            List<com.netflix.astyanax.ddl.ColumnDefinition> columnDefinitionList;
            String columnFamilyName;
            Properties properties;
            Double bloomFilterFpChance;
            String caching;
            String comment;
            Double localReadRepairChance;
            Integer gcGraceSeconds;
            Double readRepairChance;
            Boolean replicateOnWrite;
            Object populate_io_cache_on_flush;
            String compactionStrategy;
            Map<String, String> compressionOptions;
            Set<Map.Entry<String, String>> compressionOptionsCollection;
            String compressionKey;
            String compressionValue;
            String primaryKeyType;

            ColumnDefinition columnDefinition;
            String columnName;
            String columnType;

            File directory = new File(outDirectory);

            String schemaPath = directory.getPath() + File.separator + "main";
            File schemaDir = new File(schemaPath);
            if (!schemaDir.exists())
                schemaDir.mkdir();

            schemaPath = schemaPath + File.separator + "resources";
            schemaDir = new File(schemaPath);
            if (!schemaDir.exists())
                schemaDir.mkdir();

            schemaPath = schemaPath + File.separator + "schema";
            schemaDir = new File(schemaPath);
            if (!schemaDir.exists())
                schemaDir.mkdir();

            String schemaFilePath;
            PrintWriter classOut;
            try {

                for(int i = 0; i < columnFamilyDefinitionList.size(); i++ )
                 {
                     columnFamilyDefinition =  columnFamilyDefinitionList.get(i);
                     columnFamilyName = columnFamilyDefinition.getName();  //table name
                     schemaFilePath = schemaPath + File.separator + columnFamilyName + ".db";
                          classOut = new PrintWriter(new BufferedWriter(new FileWriter(schemaFilePath, false)));
                          classOut.println("CREATE TABLE " + columnFamilyName + " (");

                          primaryKeyType = generateShortHand(columnFamilyDefinition.getKeyValidationClass());
                          classOut.println("key " + primaryKeyType + " PRIMARY KEY,");

                         columnDefinitionList = columnFamilyDefinition.getColumnDefinitionList(); //ColumnDefinitionsList

                         for(int j = 0; j < columnDefinitionList.size(); j++)
                         {
                             columnDefinition = columnDefinitionList.get(j);
                             columnName = columnDefinition.getName();
                             columnType = generateShortHand(columnDefinition.getValidationClass());
                             if(j==columnDefinitionList.size() - 1)
                                 classOut.println("\"" + columnName + "\" " + columnType);
                             else
                                classOut.println("\"" + columnName + "\" " + columnType + ",");
                         }

                         //WITH COMPACTION STORAGE directive is implicit in legacy tables
                        // Spoon seems to be generating cql 2.0 (legacy) tables
                        // until further notice, all tables will include directive
                         classOut.println(") WITH COMPACT STORAGE AND");

                         bloomFilterFpChance = columnFamilyDefinition.getBloomFilterFpChance();
                         classOut.println("bloom_filter_fp_chance=" + bloomFilterFpChance + " AND");

                         caching = columnFamilyDefinition.getCaching();
                         classOut.println("caching=\'" + caching + "\' AND");

                         comment = columnFamilyDefinition.getComment();
                         classOut.println("comment=\'" + comment + "\' AND");

                         localReadRepairChance = columnFamilyDefinition.getLocalReadRepairChance();
                         classOut.println("dcLocalReadRepairChance=" + localReadRepairChance + " AND");

                         gcGraceSeconds = columnFamilyDefinition.getGcGraceSeconds();
                         classOut.println("gc_grace_seconds=" + gcGraceSeconds + " AND");

                         readRepairChance = columnFamilyDefinition.getReadRepairChance();
                         classOut.println("read_repair_chance=" + readRepairChance + " AND");

                         replicateOnWrite = columnFamilyDefinition.getReplicateOnWrite();
                         classOut.println("replicate_on_write=\'" + replicateOnWrite + "\' AND");

                         populate_io_cache_on_flush = columnFamilyDefinition.getFieldValue("POPULATE_IO_CACHE_ON_FLUSH");
                         classOut.println("populate_io_cache_flush=\'" + populate_io_cache_on_flush + "\' AND");

                         compactionStrategy = generateShortHand(columnFamilyDefinition.getCompactionStrategy());
                         classOut.println("compaction={'class': \'" + compactionStrategy + "\'} AND");

                         compressionOptions = columnFamilyDefinition.getCompressionOptions();
                         compressionOptionsCollection = compressionOptions.entrySet();
                         compressionKey = compressionOptionsCollection.iterator().next().getKey();
                         compressionValue = generateShortHand(compressionOptionsCollection.iterator().next().getValue());
                         classOut.println("compression={\'" + compressionKey + "\': \'" + compressionValue + "\'};");
                         classOut.close();
        }

        }catch (Exception ex) {
             ex.printStackTrace();

        } }catch (ConnectionException e) {
                logger.error("failed to read from C*", e);
                throw new RuntimeException("failed to read from C*", e);
            }
        }

    private String generateShortHand(String longhand) {
        if (longhand.equals("org.apache.cassandra.db.marshal.UTF8Type")) {
            return "text";
        } else if (longhand.equals("org.apache.cassandra.db.marshal.DecimalType")) {
            return "decimal";
        } else if (longhand.equals("org.apache.cassandra.db.marshal.LongType")) {
            return "bigint";
        } else if (longhand.equals("org.apache.cassandra.db.marshal.DateType")) {
            return "timestamp";
        } else if (longhand.equals("org.apache.cassandra.db.marshal.BooleanType")) {
            return "boolean";
        } else if (longhand.equals("org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy")) {
            return "SizeTieredCompactionStrategy";
        } else if (longhand.equals("org.apache.cassandra.io.compress.SnappyCompressor")) {
            return "SnappyCompressor";
        }
        return "longhand " + longhand + " not recognized";
    }
}
