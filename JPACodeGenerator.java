import org.apache.commons.lang.WordUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

public class JPACodeGenerator {

    private static String[] types = {"text", "timestamp", "decimal", "bigint", "boolean"};
    private static String[] javaTypes = {"String", "Date", "double", "int", "boolean"};

    private static final Map<String, Integer> typeMap = new HashMap<String, Integer>();
    public static final List<String> imports = new LinkedList<String>();

    static {
        for (int i = 0; i < types.length; i++) {
            typeMap.put(types[i], i);
        }

        imports.add("import javax.persistence.Column;");
        imports.add("import javax.persistence.Entity;");
        imports.add("import javax.persistence.Id;");
        imports.add("import java.util.Date;");

    }


    public String outDirectory;
    private String packageName;
    private List<String> variables;
    private List<String> getterAndSetters;
    private List<String> setterCalls;

    public JPACodeGenerator(String packageName, String outDirectory) {
        this.packageName = packageName;
        this.outDirectory = outDirectory;
        variables = new LinkedList<String>();
        getterAndSetters = new LinkedList<String>();
        setterCalls = new LinkedList<String>();
    }

    public void generateCode(Map<String, String> dataMap, String className) {

        List<String> keyList = new ArrayList<String>(dataMap.keySet().size());
        keyList.addAll(dataMap.keySet());
        Collections.sort(keyList);

        for (String key : keyList) {
            String value = dataMap.get(key);
            generateSnippets(key, typeMap.get(value), className);
        }

        File directory = new File(outDirectory);

        // Create domain, dao, impl directory
        String mainPath = directory.getAbsolutePath() + File.separator + "main";
        File mainDir = new File(mainPath);
        if (!mainDir.exists())
            mainDir.mkdir();

        String domainPath = mainPath + File.separator + "domain";
        File domainDir = new File(domainPath);
        if (!domainDir.exists())
            domainDir.mkdir();


        String daoPath = mainPath + File.separator + "dao";
        File daoDir = new File(daoPath);
        if (!daoDir.exists())
            daoDir.mkdir();


        String daoImplPath = daoPath + File.separator + "impl";
        File daoImplDir = new File(daoImplPath);
        if (!daoImplDir.exists())
            daoImplDir.mkdir();

        String servicePath = mainPath + File.separator + "service";
        File serviceDir = new File(servicePath);
        if(!serviceDir.exists())
            serviceDir.mkdir();

        String serviceImplPath = servicePath + File.separator + "impl";
        File serviceImplDir = new File(serviceImplPath);
        if(!serviceImplDir.exists())
            serviceImplDir.mkdir();


        String testPath = directory.getAbsolutePath() + File.separator + "test";
        File testDir = new File(testPath);
        if(!testDir.exists())
            testDir.mkdir();

        String domainTestPath = testPath + File.separator + "domain";
        File domainTestDir = new File(domainTestPath);
        if(!domainTestDir.exists())
            domainTestDir.mkdir();


        String daoTestPath = testPath + File.separator + "dao";
        File daoTestDir = new File(daoTestPath);
        if(!daoTestDir.exists())
            daoTestDir.mkdir();


        String serviceTestPath = testPath + File.separator + "service";
        File serviceTestDir = new File(serviceTestPath);
        if(!serviceTestDir.exists())
            serviceDir.mkdir();


        String domainClassFilePath = domainPath + File.separator + className + ".java";
        generateDomainClass(className, domainClassFilePath);

        String daoClassName = "I" + className + "Dao";
        generateDaoInterface(className, daoClassName, daoPath);

        String daoImplClassName = className + "DaoImpl";
        generateDaoImplClass(className, daoImplPath, daoClassName, daoImplClassName);

        String serviceClassName = "I" + className + "Service";
        generateServiceInterface(className, serviceClassName, servicePath);

        String serviceImplClassName = className + "ServiceImpl";
        generateServiceImplClass(className, serviceImplPath, serviceClassName, serviceImplClassName, daoClassName);

        String domainTestFilePath = domainTestPath + File.separator + className + "DomainTest.java";
        generateDomainTest(className, domainTestFilePath);

        String daoTestFilePath = daoTestPath + File.separator + className + "DaoTest.java";
        generateDaoTest(className, daoTestFilePath);

        String serviceTestFilePath = serviceTestPath + File.separator + className + "ServiceTest.java";
        generateServiceTest(className, serviceTestFilePath, serviceClassName);


    }



    private void generateDaoImplClass(String className, String daoImplPath, String daoClassName, String daoImplClassName) {
        String daoImplClassPath = daoImplPath + File.separator + daoImplClassName + ".java";
        try {
            PrintWriter classOut = new PrintWriter(new BufferedWriter(new FileWriter(daoImplClassPath, false)));
            classOut.println("package " + packageName + ".dao.impl;");
            classOut.println("\n");
            classOut.println("import javax.annotation.PostConstruct;");
            classOut.println("import org.springframework.stereotype.Repository;");

            classOut.println("import " + packageName + ".dao." + daoClassName + ";");
            classOut.println("import " + packageName + ".domain." + className + ";");
            classOut.println("import com.netflix.astyanax.entitystore.DefaultEntityManager;");

            classOut.println("\n");
            classOut.println("@Repository");
            classOut.println("public class " + daoImplClassName + " extends AbstractAstyanaxDaoImpl<" + className
                    + ", String> implements " + daoClassName + " {");

            classOut.println("\t@Override");
            classOut.println("\t@PostConstruct");
            classOut.println("\tpublic void init(){");

            classOut.println("\t\tentityManager = new DefaultEntityManager.Builder<" + className + ", String>()");
            classOut.println("\t\t\t.withEntityType(" + className + ".class).withKeyspace(cassandraServer.getKeyspace()).build();");
            classOut.println("\t}");

            classOut.println("}");

            classOut.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void generateDaoInterface(String className, String daoClassName, String daoPath) {


        String daoClassPath = daoPath + File.separator + daoClassName + ".java";
        try {
            PrintWriter classOut = new PrintWriter(new BufferedWriter(new FileWriter(daoClassPath, false)));
            classOut.println("package " + packageName + ".dao;");
            classOut.println("\n");

            classOut.println("import " + packageName + ".domain." + className + ";");
            classOut.println("\n");

            classOut.println("public interface " + daoClassName + " extends IAbstractDao<" + className + ", String> {");
            classOut.println("}");
            classOut.println("\n");

            classOut.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void generateDomainClass(String className, String domainClassFilePath) {
        try {
            PrintWriter classOut = new PrintWriter(new BufferedWriter(new FileWriter(domainClassFilePath, false)));

            classOut.println("package " + packageName + ".domain;");
            classOut.println("\n");
            printCodeSection(imports, classOut);
            classOut.println("\n");
            classOut.println("@Entity");
            classOut.println("public class " + className + " extends DomainBase {");
            generateKeyVarable(classOut);
            printCodeSection(variables, classOut);
            classOut.println("\n");
            generateKeyGetterAndSetter(classOut);
            printCodeSection(getterAndSetters, classOut);
            classOut.println("}");

            classOut.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {

            variables.clear();
            getterAndSetters.clear();

        }
    }

    private void generateDomainTest(String className, String domainTestFilePath) {
        try {
            PrintWriter classOut = new PrintWriter(new BufferedWriter(new FileWriter(domainTestFilePath, false)));
            classOut.println("package " + packageName + ".domain;");
            classOut.println("\n");
            classOut.println("import java.util.Date;");
            classOut.println("import org.junit.Before;");
            classOut.println("import org.junit.Test;");
            classOut.println("import org.junit.Assert;");
            classOut.println("\n");
            classOut.println("import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;");
            classOut.println("import com.netflix.astyanax.entitystore.DefaultEntityManager;");
            classOut.println("import com.netflix.astyanax.entitystore.EntityManager;");
            classOut.println("import com.netflix.astyanax.model.ColumnFamily;");
            classOut.println("import com.netflix.astyanax.serializers.StringSerializer;");
            classOut.println("import " + packageName + ".domain." + className + ";");
            classOut.println();
            classOut.println("public class " + className + "DomainTest extends AbstractDomainTest {");
            classOut.println();
            classOut.println("\tpublic static ColumnFamily<String, String> CF_" + className.toUpperCase() + " = ColumnFamily");
            classOut.println("\t\t.newColumnFamily(\"" + className + "\", StringSerializer.get(),");
            classOut.println("\t\t\tStringSerializer.get());");
            classOut.println();
            classOut.println("\t@Before");
            classOut.println("\tpublic void init() {");
            classOut.println("\t\tEntityManager<" + className + ", String> entityPersister = new DefaultEntityManager.Builder<"
                    + className + ", String>()");
            classOut.println("\t\t\t.withEntityType(" + className + ".class).withKeyspace(cassandraServer.getKeyspace())");
            classOut.println("\t\t\t.build();");
            classOut.println("\t\ttry {");
            classOut.println("\t\t\tentityPersister.createStorage(null);");
            classOut.println("\t\t} catch (Exception e) {");
            classOut.println("\t\t}");
            classOut.println();
            classOut.println("\t\ttry {");
            classOut.println("\t\t\tcassandraServer.getKeyspace().createColumnFamily(CF_" + className.toUpperCase() + ", null);");
            classOut.println("\t\t} catch (ConnectionException e) {");
            classOut.println("\t\t\te.printStackTrace();");
            classOut.println("\t\t}");
            classOut.println("\t}");
            classOut.println();
            classOut.println("\tprivate " + className + " create_" + className + "(String id) {");
            classOut.println("\t\t" + className + " " + className.substring(0,1).toLowerCase() + className.substring(1)
                    + " = new " + className + "();");
            classOut.println("\t\t" + className.substring(0,1).toLowerCase() + className.substring(1) + ".setId(id);");
            printCodeSection(setterCalls, classOut);
            classOut.println("\t\treturn " + className.substring(0,1).toLowerCase() + className.substring(1) + ";");
            classOut.println("\t}");
            classOut.println();
            classOut.println("\t@Test");
            classOut.println("\tpublic void basicLifecycle() throws Exception {");
            classOut.println("\t\tfinal String id = \"" + className + "_basicLifecycle\";");
            classOut.println("\t\tEntityManager<" + className + ", String> entityPersister = new DefaultEntityManager.Builder<"
                    + className + ", String>()");
            classOut.println("\t\t\t.withEntityType(" + className + ".class).withKeyspace(cassandraServer.getKeyspace())");
            classOut.println("\t\t\t.build();");
            classOut.println("\t\t" + className + " origEntity = create_" + className + "(id);");
            classOut.println();
            classOut.println("\t\tentityPersister.put(origEntity);");
            classOut.println();
            classOut.println("\t\t" + className + " getEntity = entityPersister.get(id);");
            classOut.println("\t\tSystem.out.println(getEntity.toString());");
            classOut.println("\t\tAssert.assertEquals(origEntity, getEntity);");
            classOut.println("\n");
            classOut.println("\t\tentityPersister.delete(id);");
            classOut.println("\t\tgetEntity = entityPersister.get(id);");
            classOut.println("\t\tAssert.assertEquals(null, getEntity);");
            classOut.println("\n");
            classOut.println("\t}");
            classOut.println("}");

            classOut.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void generateServiceImplClass (String className, String serviceImplPath, String serviceClassName,
                                           String serviceImplClassName, String daoClassName){
        String serviceImplClassPath = serviceImplPath + File.separator + serviceImplClassName + ".java";
        try {
            PrintWriter classOut = new PrintWriter(new BufferedWriter(new FileWriter(serviceImplClassPath, false)));
            classOut.println("package " + packageName + ".service.impl;");
            classOut.println("\n");
            classOut.println("import java.util.Collection;");
            classOut.println("import java.util.List;");
            classOut.println("import java.util.Map;\n");
            classOut.println("import org.springframework.beans.factory.annotation.Autowired;");
            classOut.println("import org.springframework.stereotype.Service;\n");
            classOut.println("import " + packageName + ".dao." + daoClassName + ";");
            classOut.println("import " + packageName + ".domain." + className + ";\n");
            classOut.println("import " + packageName + ".service." + serviceClassName + ";");
            classOut.println("@Service");
            classOut.println("public class " + serviceImplClassName + " implements " + serviceClassName + " {");
            classOut.println("\n\t@Autowired");
            classOut.println("\t" + daoClassName + " dao;\n");
            classOut.println("\t@Override");
            classOut.println("\tpublic " + className + " get(String id) {");
            classOut.println("\t\treturn dao.get(id);");
            classOut.println("\t}\n");
            classOut.println("\t@Override");
            classOut.println("\tpublic List<" + className + "> getAll() {");
            classOut.println("\t\treturn dao.getAll();");
            classOut.println("\t}\n");
            classOut.println("\t@Override");
            classOut.println("\tpublic List<" + className + "> get(Collection<String> ids) {");
            classOut.println("\t\treturn dao.get(ids);");
            classOut.println("\t}\n");
            classOut.println("\t@Override");
            classOut.println("\tpublic void save(" + className + " entity) {");
            classOut.println("\t\tdao.save(entity);\n");
            classOut.println("\t}\n");
            classOut.println("\t@Override");
            classOut.println("\tpublic void save(Collection<" + className + "> entities) {");
            classOut.println("\t\tdao.save(entities);\n");
            classOut.println("\t}\n");
            classOut.println("\t@Override");
            classOut.println("\tpublic void delete(" + className + " entity) {");
            classOut.println("\t\tdao.delete(entity);\n");
            classOut.println("\t}\n");
            classOut.println("\t@Override");
            classOut.println("\tpublic void deleteById(String id) {");
            classOut.println("\t\tdao.deleteById(id);\n");
            classOut.println("\t}\n");
            classOut.println("\t@Override");
            classOut.println("\tpublic void deleteByIds(Collection<String> ids) {");
            classOut.println("\t\tdao.deleteByIds(ids);\n");
            classOut.println("\t}\n");
            classOut.println("\t@Override");
            classOut.println("\tpublic void delete(Collection<" + className + "> entities) {");
            classOut.println("\t\tdao.delete(entities);\n");
            classOut.println("\t}\n");
            classOut.println("\t@Override");
            classOut.println("\tpublic List<" + className + "> find(java.lang.String cql) {");
            classOut.println("\t\treturn dao.find(cql);\n");
            classOut.println("\t}\n");
            classOut.println("\t@Override");
            classOut.println("\tpublic void createStorage(Map<java.lang.String, Object> options) {");
            classOut.println("\t\tdao.createStorage(options);\n");
            classOut.println("\t}\n");
            classOut.println("\t@Override");
            classOut.println("\tpublic void deleteStorage() {");
            classOut.println("\t\tdao.deleteStorage();\n");
            classOut.println("\t}\n");
            classOut.println("\t@Override");
            classOut.println("\tpublic void truncate() {");
            classOut.println("\t\tdao.truncate();\n");
            classOut.println("\t}\n");
            classOut.println();
            classOut.println("}");

            classOut.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void generateServiceInterface (String className, String serviceClassName, String servicePath) {
        String serviceClassPath = servicePath + File.separator + serviceClassName + ".java";
        try {
            PrintWriter classOut = new PrintWriter(new BufferedWriter(new FileWriter(serviceClassPath, false)));
            classOut.println("package " + packageName + ".service;\n");
            classOut.println("import org.springframework.stereotype.Service;\n");
            classOut.println("import " + packageName + ".domain." + className + ";\n");
            classOut.println("@Service");
            classOut.println("public interface " + serviceClassName + " extends IAbstractService<" + className + ", String> {\n");
            classOut.println("}");

            classOut.close();

        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void generateDaoTest(String className, String daoTestFilePath) {
        try {
            PrintWriter classOut = new PrintWriter(new BufferedWriter(new FileWriter(daoTestFilePath, false)));
            classOut.println("package " + packageName + ".dao;");
            classOut.println("\n");
            classOut.println("import java.util.ArrayList;");
            classOut.println("import java.util.Date;");
            classOut.println("import java.util.List;");
            classOut.println();
            classOut.println("import org.junit.Assert;");
            classOut.println("import org.junit.Before;");
            classOut.println("import org.junit.Test;");
            classOut.println();
            classOut.println("import " + packageName + ".dao.impl." + className + "DaoImpl;");
            classOut.println("import " + packageName + ".domain." + className + ";");
            classOut.println("import com.netflix.astyanax.entitystore.DefaultEntityManager;");
            classOut.println();
            classOut.println("public class " + className + "DaoTest extends AbstractDaoTest<" + className + ", String> {");
            classOut.println();
            classOut.println("\tprivate I" + className + "Dao dao = null;");
            classOut.println();
            classOut.println("\t@Before");
            classOut.println("\tpublic void init() {");
            classOut.println("\t\tentityManager = new DefaultEntityManager.Builder<"
                    + className + ", String>()");
            classOut.println("\t\t\t.withEntityType(" + className + ".class).withKeyspace(cassandraServer.getKeyspace())");
            classOut.println("\t\t\t.build();");
            classOut.println("\t\ttry {");
            classOut.println("\t\t\tentityManager.createStorage(null);");
            classOut.println("\t\t} catch (Exception e) {");
            classOut.println("\t\t}");
            classOut.println();
            classOut.println("\t\tdao = new " + className + "DaoImpl();");
            classOut.println("\t\tdao.setEntityManager(entityManager);");
            classOut.println();
            classOut.println("\t}");
            classOut.println();
            classOut.println("\tprivate " + className + " create_" + className + "(String id) {");
            classOut.println("\t\t" + className + " " + className.substring(0,1).toLowerCase() + className.substring(1) + " = new "
                    + className + "();");
            classOut.println("\t\t" + className.substring(0,1).toLowerCase() + className.substring(1) + ".setId(id);");
            printCodeSection(setterCalls, classOut);
            classOut.println("\t\treturn " + className.substring(0,1).toLowerCase() + className.substring(1) + ";");
            classOut.println("\t}");
            classOut.println();
            classOut.println("\t@Test");
            classOut.println("\tpublic void singleEntityTest() throws Exception {");
            classOut.println("\t\tfinal String id = \"" + className + "_ST\";");
            classOut.println("\t\t" + className + " origEntity = create_" + className + "(id);");
            classOut.println("\t\tdao.save(origEntity);");
            classOut.println();
            classOut.println("\t\t" + className + " getEntity = dao.get(id);");
            classOut.println("\t\tSystem.out.println(getEntity.toString());");
            classOut.println("\t\tAssert.assertEquals(origEntity, getEntity);");
            classOut.println();
            classOut.println("\t\tdao.deleteById(id);");
            classOut.println("\t\tgetEntity = dao.get(id);");
            classOut.println("\t\tAssert.assertEquals(null, getEntity);");
            classOut.println();
            classOut.println("\t\tfinal String idnew = \"" + className + "_STnew\";");
            classOut.println("\t\t" + className + " newEntity = create_" + className + "(idnew);");
            classOut.println("\t\t dao.save(newEntity);");
            classOut.println();
            classOut.println("\t\tgetEntity = dao.get(idnew);");
            classOut.println("\t\tSystem.out.println(getEntity.toString());");
            classOut.println("\t\tAssert.assertEquals(newEntity, getEntity);");
            classOut.println();
            classOut.println("\t\tdao.delete(newEntity);");
            classOut.println("\t\tgetEntity = dao.get(idnew);");
            classOut.println("\t\tAssert.assertEquals(null, getEntity);");
            classOut.println();
            classOut.println("\t}");
            classOut.println();
            classOut.println("\tprivate List<" + className + "> createMultiple_" + className + "(String id) {");
            classOut.println("\t\tList<" + className + "> list = new ArrayList<" + className + ">();");
            classOut.println("\t\t" + className + " " + className.substring(0,1).toLowerCase() + className.substring(1) + " = null;");
            classOut.println();
            classOut.println("\t\tfor (int i = 0; i < 10; i++) {");
            classOut.println("\t\t\t" + className.substring(0,1).toLowerCase() + className.substring(1) + " = new " + className + "();");
            classOut.println("\t\t\t" + className.substring(0,1).toLowerCase() + className.substring(1) + ".setId(id + i);");
            printCodeSection(setterCalls, classOut);
            classOut.println();
            classOut.println("\t\t\tlist.add(" + className.substring(0,1).toLowerCase() + className.substring(1) + ");");
            classOut.println("\t\t}");
            classOut.println();
            classOut.println("\treturn list;");
            classOut.println("\t}");
            classOut.println();
            classOut.println("\t@Test");
            classOut.println("\tpublic void multipleEntityTest() throws Exception {");
            classOut.println("\t\tList<" + className + ">  getfirstList = dao.getAll();");
            classOut.println("\t\tdao.delete(getfirstList);");
            classOut.println("\t\tgetfirstList = dao.getAll();");
            classOut.println("Assert.assertEquals(0, getfirstList.size());");
            classOut.println();
            classOut.println("\t\tString id = \"" + className + "_MT\";");
            classOut.println("\t\tList<" + className + "> " + className + "List = createMultiple_" + className + "(id);");
            classOut.println("\t\tdao.save(" + className + "List);");
            classOut.println();
            classOut.println("\t\tList<" + className + ">  get" + className + "List = dao.getAll();");
            classOut.println("\t\tAssert.assertEquals(" + className + "List.size(), get" + className + "List.size());");
            classOut.println();
            classOut.println("\t\tdao.delete(" + className + "List);");
            classOut.println("\t\tget" + className + "List = dao.getAll();");
            classOut.println("\t\tAssert.assertEquals(0,get" + className + "List.size());");
            classOut.println("\t}");
            classOut.println("}");

            classOut.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    private void generateServiceTest (String className, String serviceTestFilePath, String serviceClassName){
        try{
            PrintWriter classOut = new PrintWriter(new BufferedWriter(new FileWriter(serviceTestFilePath, false)));
            classOut.println("package " + packageName + ".service;");
            classOut.println("\n");
            classOut.println("import java.util.*;\n");
            classOut.println("import org.junit.Assert;");
            classOut.println("import org.junit.Before;");
            classOut.println("import org.junit.Test;");
            classOut.println("import org.springframework.beans.factory.annotation.Autowired;\n");
            classOut.println("import " + packageName + ".domain." + className + ";\n");
            classOut.println("public class " + className + "ServiceTest extends AbstractServiceTest<" + className + ", String> {\n\n\n");
            classOut.println("\t@Autowired");
            classOut.println("\t" + serviceClassName +" service;\n");
            classOut.println("\tprivate " + className + " create_" + className + "(String id) {");
            String classNameVariable = className.substring(0, 1).toLowerCase()
                    + className.substring(1);
            classOut.println("\t\t" + className + " " + classNameVariable + " = new " + className + "();");
            classOut.println("\t\t" + classNameVariable + ".setId(id);");
            printCodeSection(setterCalls, classOut);
            classOut.println();
            classOut.println("\t\treturn " + classNameVariable + ";");
            classOut.println("\t}\n");
            classOut.println("\t@Before");
            classOut.println("\tpublic void createStorage(){");
            classOut.println("\t\ttry{");
            classOut.println("\t\t service.createStorage(null);");
            classOut.println("\t\t}catch(Exception e){");
            classOut.println("\t\t\t//e.printStackTrace();");
            classOut.println("\t\t\tSystem.out.println(e.getMessage());");
            classOut.println("\t\t}");
            classOut.println("\t}\n");
            classOut.println("\t@Test");
            classOut.println("\tpublic void lifecycleTest() {");
            classOut.println("\t\tfinal String id = \"" + className + "_STT\";");
            classOut.println("\t\t" + className + " origEntity = create_" + className + "(id);");
            classOut.println("\t\tservice.save(origEntity);\n");
            classOut.println("\t\t" + className + " getEntity =  service.get(id);");
            classOut.println("\t\tSystem.out.println(getEntity.toString());");
            classOut.println("\t\tAssert.assertEquals(origEntity, getEntity);\n");
            classOut.println("\t\tservice.deleteById(id);");
            classOut.println("\t\tgetEntity =  service.get(id);");
            classOut.println("\t\tAssert.assertEquals(null, getEntity);\n");
            classOut.println("\t\tfinal String idnew = \"" + className + "_STnew\";");
            classOut.println("\t\t" + className + " newEntity = create_" + className + "(idnew);");
            classOut.println("\t\tservice.save(newEntity);\n");
            classOut.println("\t\tgetEntity =  service.get(idnew);");
            classOut.println("\t\tSystem.out.println(getEntity.toString());");
            classOut.println("\t\tAssert.assertEquals(newEntity, getEntity);\n");
            classOut.println("\t\tservice.delete(newEntity);");
            classOut.println("\t\tgetEntity =  service.get(idnew);");
            classOut.println("\t\tAssert.assertEquals(null, getEntity);");
            classOut.println("\t}\n");
            classOut.println("\tprivate List<" + className + "> createMultiple_" + className + "(String id) {");
            classOut.println("List<" + className + "> list = new ArrayList<" + className + ">();");
            classOut.println(className + " " + classNameVariable + " = null;\n");
            classOut.println("\t\tfor (int i = 0; i < 10; i++) {");
            classOut.println("\t\t\t" + classNameVariable + " = new " + className + "();");
            classOut.println("\t\t\t" + classNameVariable + ".setId(id + i);");
            printCodeSection(setterCalls, classOut);
            classOut.println("list.add(" + classNameVariable + ");");
            classOut.println("\t\t}\n");
            classOut.println("\t\treturn list;");
            classOut.println("\t}\n");
            classOut.println("\t@Test");
            classOut.println("\tpublic void multipleEntityTest() throws Exception {");
            classOut.println("\t\tString id = \"" + className + "MT\";");
            classOut.println("\t\tList<" + className + ">  " + className + "List = createMultiple_" + className + "(id);");
            classOut.println("\t\tservice.save(" + className + "List);\n");
            classOut.println("\t\tList<" + className + ">  get" + className + "List = service.getAll();");
            classOut.println("\t\tAssert.assertEquals(" + className + "List.size(), get" + className + "List.size());\n");
            classOut.println("\t\tservice.delete(" + className  + "List);");
            classOut.println("\t\tget" + className + "List = service.getAll();");
            classOut.println("\t\tAssert.assertEquals(0,get" + className + "List.size());");
            classOut.println("\t}\n");
            classOut.println("}");

            classOut.close();


        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {

            setterCalls.clear();

        }
    }

    private void generateKeyVarable(PrintWriter out) throws Exception {
        out.println("\t@Id");
        out.println("\tprivate String id;");
    }

    private void generateKeyGetterAndSetter(PrintWriter out) throws Exception {

        out.println("\tpublic String getId() {");
        out.println("\t\treturn id;");
        out.println("\t}\n");

        out.println("\tpublic void setId(String id) {");
        out.println("\t\tthis.id = id;");
        out.println("\t}\n");

    }

    private void printCodeSection(List<String> section, PrintWriter out) throws Exception {
        for (String line : section) {
            out.println(line);
        }
    }

    private void generateSnippets(String name, int type, String className) {

        name = name.toLowerCase();
        /* if column name contains -, remove it */
        String variablename = name.replace("-", "_").replace("&", "");

        StringBuilder sb = new StringBuilder();
        sb.append("\t@Column(name=\"").append(name.toUpperCase()).append("\")\n");
        sb.append("\tprivate ");
        sb.append(javaTypes[type]);
        sb.append(" ");
        sb.append(variablename);
        sb.append(";\n");
        variables.add(sb.toString());

        sb = new StringBuilder();
        sb.append("\tpublic ").append(javaTypes[type]).append(" get").append(WordUtils.capitalize(variablename))
                .append("() {\n");
        sb.append("\t\treturn ").append(variablename).append(";\n");
        sb.append("\t}\n\n");

        sb.append("\tpublic void ").append("set").append(WordUtils.capitalize(variablename))
                .append("(").append(javaTypes[type]).append(" ").append(variablename).append(") {\n");
        sb.append("\t\tthis.").append(variablename).append(" = ").append(variablename).append(";\n");
        sb.append("\t}\n\n");
        getterAndSetters.add(sb.toString());

        sb = new StringBuilder();
        sb.append("\t\t").append(className.substring(0,1).toLowerCase()).append(className.substring(1)).append(".set")
                .append(WordUtils.capitalize(variablename))
                .append("(").append(generateArgumentString(type, variablename)).append(");");

        setterCalls.add(sb.toString());

    }

    private String generateArgumentString (int type, String variablename) {


        switch (type) {

            case 0 : return '"' + variablename.substring(0, 1).toLowerCase()
                    + variablename.substring(1).toUpperCase() + '"';

            case 1 : return "new Date()";

            case 2 : return "1.1";

            case 3 : return "1";

            case 4 : return "true";
        }
        return "ERROR";
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
