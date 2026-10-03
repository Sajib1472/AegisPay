package com.aegispay.app.platform.ops;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class GitInfoContributor implements InfoContributor {

    private final String commit;
    private final String engine;

    public GitInfoContributor(
            @Value("${aegispay.build.commit:dev}") String commit,
            @Value("${info.app.engine:0.1.0}") String engine
    ) {
        this.commit = commit;
        this.engine = engine;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("git", java.util.Map.of("commit", commit));
        builder.withDetail("engine", engine);
    }
}
