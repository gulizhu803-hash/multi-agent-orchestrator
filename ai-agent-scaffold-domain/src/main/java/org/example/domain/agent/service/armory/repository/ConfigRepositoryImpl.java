package org.example.domain.agent.service.armory.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
@Repository
public class ConfigRepositoryImpl implements IConfigRepository {

    private static final String OUTPUT_DIR = "data/agent-configs";

    @Override
    public String save(String appName, String yamlContent) throws Exception {
        String filename = appName + ".yml";
        Path outputDir = Paths.get(OUTPUT_DIR);
        Files.createDirectories(outputDir);
        Path outputFile = outputDir.resolve(filename);
        Files.writeString(outputFile, yamlContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("智能体配置文件已保存: {}", outputFile.toAbsolutePath());
        return filename;
    }

    @Override
    public void delete(String appName) throws Exception {
        Path outputFile = Paths.get(OUTPUT_DIR, appName + ".yml");
        Files.deleteIfExists(outputFile);
        log.warn("配置文件 {} 已删除，需重启应用以完全卸载智能体", outputFile.toAbsolutePath());
    }
}
