package net.es.oscars.pss.tpl;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.*;

import lombok.extern.slf4j.Slf4j;
import net.es.oscars.app.props.PssProperties;
import net.es.oscars.pss.beans.TemplateOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class Stringifier {

    private Configuration fmCfg;

    private PssProperties props;

    @Autowired
    public Stringifier(PssProperties props) {
        this.props = props;
        this.configureTemplates(false);
    }

    public void configureTemplates(boolean ignoreErrors) {

        fmCfg = new Configuration(Configuration.VERSION_2_3_22);
        fmCfg.setDefaultEncoding("UTF-8");
        fmCfg.setObjectWrapper(new DefaultObjectWrapper(Configuration.VERSION_2_3_22));
        fmCfg.setNumberFormat("computer");
        if (ignoreErrors) {
            // this is used to suppress errors during some tests
            fmCfg.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
            fmCfg.setLogTemplateExceptions(false);
        }

        List<TemplateLoader> loaderList = new ArrayList<>();


            for (String templatePath : this.props.getTemplateDirs()) {
            try {
                FileTemplateLoader ftl = new FileTemplateLoader(new File(templatePath));
                loaderList.add(ftl);

            } catch (IOException ex) {
                log.error("IO exception for "+templatePath, ex);
            }
            log.info("will load templates from "+templatePath);

        }

        MultiTemplateLoader mtl = new MultiTemplateLoader(loaderList.toArray(new TemplateLoader[0]));
        fmCfg.setTemplateLoader(mtl);
    }

    public TemplateOutput stringify(Map<String, Object> root, String templateFilename) throws IOException, TemplateException {

        Writer writer = new StringWriter();
        Template tpl = fmCfg.getTemplate(templateFilename);
        tpl.process(root, writer);
        writer.flush();
        String unprocessed = writer.toString();
        List<String> unprocessedLines = Arrays.asList(unprocessed.split("[\\r\\n]+"));
        List<String> processedLines = new ArrayList<>();
        String templateVersion = null;
        boolean hasVersion = false;

        for (String line : unprocessedLines) {
            if (line.startsWith("@version")) {
                List<String> parts = Arrays.asList(line.split(":+"));
                if (parts.size() == 2) {
                    templateVersion = parts.get(1).replaceAll("\\s+", "");
                    if (templateVersion.length() > 0) {
                        hasVersion = true;
                    }
                } else {
                    log.error("invalid version line in "+templateFilename+" : [ "+line+" ]");
                }
            } else {
                processedLines.add(line);
            }
        }
        String processed = String.join("\n", processedLines);

        return TemplateOutput.builder()
                .unprocessed(unprocessed)
                .unprocessedLines(unprocessedLines)
                .processed(processed)
                .processedLines(processedLines)
                .hasVersion(hasVersion)
                .templateVersion(templateVersion)
                .build();


    }

}
