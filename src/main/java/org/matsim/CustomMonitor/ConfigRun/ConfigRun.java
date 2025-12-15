package org.matsim.CustomMonitor.ConfigRun;

import org.springframework.core.io.Resource;

public class ConfigRun {

    private final Resource csvResourceHub;
    private final Resource csvResourceEv;
    private final String configPath;
    private final boolean publishOnSpring;
    private final boolean debug;
    private final Double sampleSizeStatic;

    // Costruttore privato per il builder
    private ConfigRun(Builder builder) {
        this.sampleSizeStatic = builder.sampleSizeStatic;
        this.csvResourceHub = builder.csvResourceHub;
        this.csvResourceEv = builder.csvResourceEv;
        this.configPath = builder.configPath;
        this.publishOnSpring = builder.publishOnSpring;
        this.debug = builder.debug;
    }

    // Getter per accedere ai campi
    public Resource getCsvResourceHub() {
        return csvResourceHub;
    }

    public Resource getCsvResourceEv() {
        return csvResourceEv;
    }

    public String getConfigPath() {
        return configPath;
    }

    public boolean isPublishOnSpring() {
        return publishOnSpring;
    }

    public boolean isDebug() {
        return debug;
    }

    public Double GetsampleSizeStatic(){
        return sampleSizeStatic;
    }

    // Builder interno
    public static class Builder {
        private Resource csvResourceHub;
        private Resource csvResourceEv;
        private String configPath;
        private boolean publishOnSpring = false; // default
        private boolean debug = false;           // default
        private Double sampleSizeStatic;

        public Builder csvResourceHub(Resource csvResourceHub) {
            this.csvResourceHub = csvResourceHub;
            return this;
        }

        public Builder csvResourceEv(Resource csvResourceEv) {
            this.csvResourceEv = csvResourceEv;
            return this;
        }

        public Builder configPath(String configPath) {
            this.configPath = configPath;
            return this;
        }

        public Builder publishOnSpring(boolean publishOnSpring) {
            this.publishOnSpring = publishOnSpring;
            return this;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Builder sampleSizeStatic(Double sampleSizeStatic){
            this.sampleSizeStatic = sampleSizeStatic;
            return this;
        }

        public ConfigRun build() {
            // Puoi aggiungere validazioni qui
            if (configPath == null || configPath.isEmpty()) {
                throw new IllegalStateException("Config path must be set");
            }
            return new ConfigRun(this);
        }
    }

    // Metodo statico helper per iniziare il builder
    public static Builder builder() {
        return new Builder();
    }
}