/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.beeline.architecting_graph.dto.ErrorResponseDto;
import ru.beeline.architecting_graph.dto.WorkspaceValidationResponseDto;
import ru.beeline.architecting_graph.exception.ValidationException;
import ru.beeline.architecting_graph.service.WorkspaceValidationService;
@RestController
@RequestMapping("/api/v1/workspace")
@RequiredArgsConstructor
public class WorkspaceValidationController {

    private final WorkspaceValidationService workspaceValidationService;

    @GetMapping("/validate/{doc-id}")
    @Operation(summary = "Валидация рабочего пространства из документа")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Валидация прошла успешно",
                    content = @Content(schema = @Schema(implementation = WorkspaceValidationResponseDto.class))),

            @ApiResponse(responseCode = "400",
                    description =
                            "Ошибка валидации входных данных/документа. В `message` возвращается конкретная причина.\n\n"
                                    + "Возможные значения `message` (в точности как в ответе API):\n"
                                    + "- `Неверный формат идентификатора документа`\n"
                                    + "- `Файл не соответствует json формату`\n"
                                    + "- `В файле отсутствует описание Model`\n"
                                    + "- `Для Model не задано обязательное свойство `workspace_cmdb``\n"
                                    + "- `В Model не описан ни один SoftwareSystem`\n"
                                    + "- `Не описан SoftwareSystem cо свойством `cmdb` равным `workspace_cmdb` из свойства Model`\n"
                                    + "- `Описано несколько SoftwareSystem cо свойством `cmdb` равным `workspace_cmdb` из свойства Model`\n"
                                    + "- `Описан SoftwareSystem без заполненного name`\n"
                                    + "- `Для контейнера системы <указана name родительской softwareSystem> не " +
                                    "указан name`\n"
                                    + "- `Для компонента контейнера <указана name родительской Container> в системе " +
                                    "<указана name родительской softwareSystem> не указан name`\n"
                                    + "- `Не для всех deploymentNode указан name`\n"
                                    + "- `Не для всех deploymentNode указан environment`\n"
                                    + "- `Не для всех containerInstance указан environment`\n"
                                    + "- `Не для всех containerInstance указан развернутый Container`\n"
                                    + "- `Не для всех infrastructureNodes указан environment`\n"
                                    + "- `Не для всех infrastructureNodes указан name`",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "invalid-doc-id-format",
                                            summary = "doc-id не число",
                                            value = "{\"message\":\"Неверный формат идентификатора документа\"}"
                                    ),
                                    @ExampleObject(
                                            name = "json-parse-error",
                                            summary = "Невалидный JSON",
                                            value = "{\"message\":\"Файл не соответствует json формату\"}"
                                    ),
                                    @ExampleObject(
                                            name = "model-missing",
                                            summary = "Нет model",
                                            value = "{\"message\":\"В файле отсутствует описание Model\"}"
                                    ),
                                    @ExampleObject(
                                            name = "workspace-cmdb-missing",
                                            summary = "Не задан model.properties.workspace_cmdb",
                                            value = "{\"message\":\"Для Model не задано обязательное свойство `workspace_cmdb`\"}"
                                    ),
                                    @ExampleObject(
                                            name = "software-systems-missing",
                                            summary = "Нет softwareSystems",
                                            value = "{\"message\":\"В Model не описан ни один SoftwareSystem\"}"
                                    ),
                                    @ExampleObject(
                                            name = "deployment-node-missing-environment",
                                            summary = "deploymentNode без environment",
                                            value = "{\"message\":\"Не для всех deploymentNode указан environment\"}"
                                    ),
                                    @ExampleObject(
                                            name = "container-instance-missing-containerId",
                                            summary = "containerInstance без containerId",
                                            value = "{\"message\":\"Не для всех containerInstance указан развернутый Containert\"}"
                                    )
                            }
                    )),

            @ApiResponse(responseCode = "403",
                    description = "Нет прав на получение документа",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(
                                    name = "forbidden",
                                    value = "{\"message\":\"Нет прав на получение документа\"}"
                            )
                    )),
            @ApiResponse(responseCode = "404",
                    description = "Файла с указанным id не существует",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(
                                    name = "not-found",
                                    value = "{\"message\":\"Файла с указанным id не существует\"}"
                            )
                    )),
            @ApiResponse(responseCode = "503",
                    description = "Ошибка при получении документа",
                    content = @Content(
                            schema = @Schema(implementation = ErrorResponseDto.class),
                            examples = @ExampleObject(
                                    name = "document-service-unavailable",
                                    value = "{\"message\":\"Ошибка при получении документа\"}"
                            )
                    ))
    })
    public ResponseEntity<WorkspaceValidationResponseDto> validate(@PathVariable("doc-id") String docIdRaw) {
        long docId;
        try {
            docId = Long.parseLong(docIdRaw);
        } catch (Exception e) {
            throw new ValidationException("Неверный формат идентификатора документа");
        }

        WorkspaceValidationResponseDto result = workspaceValidationService.validateByDocId(docId);
        return ResponseEntity.ok(result);
    }
}

