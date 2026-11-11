package com.bookmuse.app.config;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;
@Configuration
@OpenAPIDefinition(info=@Info(title="BookMuse API",version="1.0",
    description="Administrator accounts and administrator-authored community resources are visible only to ADMIN callers. Other roles receive filtered lists/counts and 404 for hidden resource IDs. The book catalogue remains visible to authenticated users."),security=@SecurityRequirement(name="bearerAuth"))
@SecurityScheme(name="bearerAuth",type=SecuritySchemeType.HTTP,scheme="bearer",bearerFormat="JWT")
public class OpenApiConfig {}
