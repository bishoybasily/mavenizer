package com.gmail.bishoybasily.mavenizer;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * @author bishoybasily
 * @since 2020-05-30
 */
public class Main {

    public static void main(String[] args) {

        Main main = new Main();

        String
                modulePath = "/home/bishoybasily/Projects/ibm/enpo/Counter/CounterCore",
                group = "com.ibm.enpo",
                version = "1.0.0-SNAPSHOT";

        main.extractJars(modulePath)
                .flatMap(it -> main.createDependency(group, version, it))
                .subscribe(System.out::println);

    }

    private Mono<String> createDependency(String group, String version, File it) {
        return Mono.fromCallable(() -> {

            String artifactId = extractArtifactId(it.getName());

            String command = String.format("mvn deploy:deploy-file -DgroupId=%s -DartifactId=%s -Dversion=%s -DgeneratePom=true -Dpackaging=jar -DrepositoryId=nexus -Durl=http://localhost:8081/repository/maven-snapshots -Dfile=%s", group, artifactId, version, it.getAbsolutePath());

            executeCommand(command);

            StringBuilder builder = new StringBuilder()
                    .append("<dependency>\n")
                    .append(String.format("\t<groupId>%s</groupId>\n", group))
                    .append(String.format("\t<artifactId>%s</artifactId>\n", artifactId))
                    .append(String.format("\t<version>%s</version>\n", version))
                    .append("</dependency>\n");
            return builder.toString();

        });
    }

    private Flux<File> extractJars(String modulePath) {
        return Flux.create(sink -> {

            try {

                File moduleDirectory = new File(modulePath);

                File classpath = new File(moduleDirectory + File.separator + ".classpath");
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(classpath);

                doc.getDocumentElement().normalize();

                NodeList nList = doc.getElementsByTagName("classpathentry");
                for (int i = 0; i < nList.getLength(); i++) {

                    Node nNode = nList.item(i);

                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {

                        Element eElement = (Element) nNode;

                        if (eElement.getAttribute("kind").equals("lib")) {
                            sink.next(new File(moduleDirectory.getParent() + eElement.getAttribute("path")));
                        }

                    }

                }

                sink.complete();

            } catch (Exception e) {
                sink.error(e);
            }

        });
    }

    private String extractArtifactId(String s) {
        return s.substring(0, s.lastIndexOf(".")).replaceAll("\\.", "-");
    }

    private void executeCommand(String cmd) {
        try {

            InputStream is = Runtime.getRuntime().exec(cmd).getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Set<File> findJars(File file) {

        Set<File> paths = new HashSet<>();

        for (File f : file.listFiles()) {

            if (f.isFile() && f.getName().toLowerCase().endsWith(".jar"))
                paths.add(f);

            if (f.isDirectory())
                paths.addAll(findJars(f));

        }

        return paths;

    }

}
