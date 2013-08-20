import org.apache.commons.lang.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

/**
 * This class will read all schema file from "resources/schema" and generate JPA class.
 */

public class CassandraDataTypeMapper {

    public static final String CLASSNAME = "classname";

    @Autowired
    private JPACodeGenerator codeGenerator;

    public void generate(File schemaDir) throws JPAGenerationException {

        if (!schemaDir.isDirectory()) {
            throw new JPAGenerationException();
        }

        File[] schemaFiles = schemaDir.listFiles();
        if (schemaFiles == null || schemaFiles.length == 0)
            return;

        for (File schemaFile : schemaFiles) {
            try {
                Map<String, String> map = mapDataTypes(schemaFile);
                String classNameRaw = map.remove(CLASSNAME);
                String [] tokens = classNameRaw.split("_");

                StringBuilder sb = new StringBuilder();
                for(String token: tokens) {
                    sb.append(WordUtils.capitalize(token.toLowerCase()));
                }
                codeGenerator.generateCode(map, sb.toString());
            } catch (Exception ex) {
                throw new JPAGenerationException();
            }
        }


    }

    public Map<String, String> mapDataTypes(File schemaFile) throws Exception {

        BufferedReader br = new BufferedReader(new FileReader(schemaFile));
        String s = "";
        while (br.ready()) {
            s += br.readLine();
        }
        System.out.println(s);
        int firstBreak = s.indexOf("(");
        int lastBreak = s.lastIndexOf(")");

        String prefix = s.substring(0, firstBreak);
        String body = s.substring(firstBreak + 1, lastBreak);

        String[] tokens = prefix.split(" ");
        String className = tokens[tokens.length - 1].trim();
        className = WordUtils.capitalize(className);

        String pattern1 = "(substring\\()(\\w+)(,.*?\\))";
        String pattern2 = "(substr\\()(\\w+)(,.*?\\))";
        body = body.replaceAll(pattern1, "$2");
        body = body.replaceAll(pattern2, "$2");

        Map<String, String> metaDataMap = new HashMap<String, String>();
        tokens = body.split(",");
        for (String pairStr : tokens) {
            pairStr = pairStr.replace("\"", "").replace("\n", "").trim();
            String[] valuePair = pairStr.split(" ");
            if (valuePair.length != 2)
                continue;
            String key = valuePair[0];
            String type = valuePair[1];
            int indexSubstrMethod = key.indexOf("(");
            if (indexSubstrMethod > 0) {
                int indexMethodParam = type.indexOf(",");
                key = type.substring(indexSubstrMethod + 1, indexMethodParam);
            }
            metaDataMap.put(key, type);
            //printMap(metaDataMap);
        }
        metaDataMap.put(CLASSNAME, className);

        return metaDataMap;

    }

    private void printMap(Map<String, String> map) {
        int count = 0;
        for (String key : map.keySet()) {
            System.out.println(key + " => " + map.get(key));
            count++;
        }
        System.out.println("total values: " + count);

    }


    public JPACodeGenerator getCodeGenerator() {
        return codeGenerator;
    }

    public void setCodeGenerator(JPACodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
    }
}
