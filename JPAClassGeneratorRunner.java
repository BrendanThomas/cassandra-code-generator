import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import java.io.File;
import java.net.URL;

//Initiates SchemaFileGenerator, CassandraDataTypeMapper, JPACodeGenerator

public class JPAClassGeneratorRunner {

    protected static ApplicationContext getApplicationContext() {
        return new ClassPathXmlApplicationContext(
                "classpath:/META-INF/appContext.xml");
    }

    public static void main(String[] args) throws Exception {

        ApplicationContext context = getApplicationContext();

        SchemaFileGenerator schemaFileGenerator = (SchemaFileGenerator) context.getBean("schemaCodeGenerator");

        schemaFileGenerator.init();

        schemaFileGenerator.generateCode();

        Resource resource = context.getResource("classpath:/schema");


        CassandraDataTypeMapper generator = (CassandraDataTypeMapper) context.getBean("jPAClassGenerator");

        try {
            generator.generate(resource.getFile());
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }


}
