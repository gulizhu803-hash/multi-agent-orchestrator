package org.example.trigger.http;

import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.example.api.dto.AgentConfigDTO;
import org.example.api.response.Response;
import org.example.domain.agent.model.valobj.AgentConfigVO;
import org.example.domain.agent.IConfigService;
import org.example.domain.agent.model.entity.ConfigSaveCommandEntity;
import org.example.types.enums.ResponseCode;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-config")
@CrossOrigin(origins = "*")
public class AgentConfigController {

    @Resource
    private IConfigService configService;

    @PostMapping("/save")
    public Response<String> saveAgentConfig(@RequestBody AgentConfigDTO configDTO) {
        try {
            AgentConfigVO agentConfig = BeanUtil.copyProperties(configDTO, AgentConfigVO.class);
            ConfigSaveCommandEntity command = ConfigSaveCommandEntity.builder()
                    .agentConfig(agentConfig)
                    .build();
            String filename = configService.saveAgentConfig(command);
            return Response.<String>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("智能体配置保存成功，已热装配")
                    .data(filename)
                    .build();
        } catch (Exception e) {
            log.error("保存智能体配置失败", e);
            return Response.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("保存失败: " + e.getMessage())
                    .build();
        }
    }

    @GetMapping("/list")
    public Response<List<AgentConfigDTO>> listAgentConfigs() {
        try {
            List<AgentConfigVO> agentConfigs = configService.listAgentConfigs();
            List<AgentConfigDTO> dtoList = agentConfigs.stream()
                    .map(config -> BeanUtil.copyProperties(config, AgentConfigDTO.class))

                    .collect(Collectors.toList());
            return Response.<List<AgentConfigDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(dtoList)
                    .build();
        } catch (Exception e) {
            log.error("查询配置列表失败", e);
            return Response.<List<AgentConfigDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("查询失败")
                    .build();
        }
    }

    @DeleteMapping("/delete/{appName}")
    public Response<Void> deleteAgentConfig(@PathVariable String appName) {
        try {
            configService.deleteAgentConfig(appName);
            return Response.<Void>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("配置已删除，重启后生效")
                    .build();
        } catch (Exception e) {
            log.error("删除配置失败", e);
            return Response.<Void>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("删除失败")
                    .build();
        }
    }
}
