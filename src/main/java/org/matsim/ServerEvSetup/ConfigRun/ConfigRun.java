package org.matsim.ServerEvSetup.ConfigRun;

import org.springframework.core.io.Resource;

public class ConfigRun {

    /*
    * Alcune strategie non sono implementate ma per ora sono placeholder 
    */
    public enum VehicleGenerationStrategyEnum {
        UNIFORM,
        NORMAL,
        FROM_CSV
    }

    public enum PlanGenerationStrategyEnum {
        RANDOM,
        STATIC,
        FROM_CSV
    }

    public enum HubGenerationStrategyEnum {
        FIXED,
        RANDOM,
        FROM_CSV
    }

    /* =======================
       Risorse
       ======================= */
    private final Resource csvResourceHub;
    private final Resource csvResourceEv;

    /* =======================
       Setup run
       ======================= */
    private final Double  sampleSizeStatic;
    private final Double  stepSize;
    private final String  configPath;
    private final boolean debugLink;
    private final boolean realTime;

    /* =======================
       Setup ws
       ======================= */
    private final Long publisherRateMs;
    private final boolean publisherDirty;

    /* =======================
       Dati veicoli / SOC
       ======================= */
    private final Integer numeroVeicoli;
    private final Double socMedio;
    private final Double socStdDev;
    private final Double targetSocMean;
    private final Double targetSocStdDev;

    /* =======================
       Strategie di generazione
       ======================= */
    private final VehicleGenerationStrategyEnum vehicleStrategy;
    private final PlanGenerationStrategyEnum planStrategy;
    private final HubGenerationStrategyEnum hubStrategy;

    private ConfigRun(Builder builder) {
        this.csvResourceHub = builder.csvResourceHub;
        this.csvResourceEv = builder.csvResourceEv;

        this.sampleSizeStatic = builder.sampleSizeStatic;
        this.stepSize = builder.stepSize;
        this.configPath = builder.configPath;
        this.debugLink = builder.debugLink;
        this.realTime = builder.realTime;

        this.numeroVeicoli = builder.numeroVeicoli;
        this.socMedio = builder.socMedio;
        this.socStdDev = builder.socStdDev;
        this.targetSocMean = builder.targetSocMean;
        this.targetSocStdDev = builder.targetSocStdDev;

        this.vehicleStrategy = builder.vehicleStrategy;
        this.planStrategy = builder.planStrategy;
        this.hubStrategy = builder.hubStrategy;

        this.publisherRateMs = builder.publisherRateMs;
        this.publisherDirty  = builder.publisherDirty;
    }

    /* =======================
       Getter
       ======================= */

    public VehicleGenerationStrategyEnum getVehicleStrategy() {
        return vehicleStrategy;
    }

    public PlanGenerationStrategyEnum getPlanStrategy() {
        return planStrategy;
    }

    public HubGenerationStrategyEnum getHubStrategy() {
        return hubStrategy;
    }

    public Integer getNumeroVeicoli() {
        return numeroVeicoli;
    }

    public Double getSocMedio() {
        return socMedio;
    }

    public Double getSocStdDev() {
        return socStdDev;
    }

    public Double getTargetSocMean() {
        return targetSocMean;
    }

    public Double getTargetSocStdDev() {
        return targetSocStdDev;
    }

    public Resource getCsvResourceHub() {
        return csvResourceHub;
    }

    public Resource getCsvResourceEv() {
        return csvResourceEv;
    }

    public Double getSampleSizeStatic() {
        return sampleSizeStatic;
    }

    public Double getStepSize(){
        return stepSize;
    }

    public String getConfigPath() {
        return configPath;
    }

    public boolean isDebugLink() {
        return debugLink;
    }

    public long publisherRateMs(){
        return publisherRateMs;
    }

    public boolean publisherDirty(){
        return publisherDirty;
    }

    public boolean isRealTime(){
        return realTime;
    }

    /* =======================
       Builder
       ======================= */

    public static class Builder {
        private Resource csvResourceHub;
        private Resource csvResourceEv;

        private String configPath;
        private boolean debugLink = false;
        private boolean realTime = false;
        private Double sampleSizeStatic;
        private Double stepSize;

        private Integer numeroVeicoli;
        private Double socMedio;
        private Double socStdDev;
        private Double targetSocMean;
        private Double targetSocStdDev;

        private VehicleGenerationStrategyEnum vehicleStrategy;
        private PlanGenerationStrategyEnum planStrategy;
        private HubGenerationStrategyEnum hubStrategy;

        private Long publisherRateMs;
        private boolean publisherDirty;

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

        public Builder debugLink(boolean debugLink) {
            this.debugLink = debugLink;
            return this;
        }

        public Builder sampleSizeStatic(Double sampleSizeStatic) {
            this.sampleSizeStatic = sampleSizeStatic;
            return this;
        }

        public Builder stepSize(Double stepSize){
            this.stepSize = stepSize;
            return this;
        }

        public Builder numeroVeicoli(Integer numeroVeicoli) {
            this.numeroVeicoli = numeroVeicoli;
            return this;
        }

        public Builder socMedio(Double socMedio) {
            this.socMedio = socMedio;
            return this;
        }

        public Builder socStdDev(Double socStdDev) {
            this.socStdDev = socStdDev;
            return this;
        }

        public Builder targetSocMean(Double targetSocMean) {
            this.targetSocMean = targetSocMean;
            return this;
        }

        public Builder targetSocStdDev(Double targetSocStdDev) {
            this.targetSocStdDev = targetSocStdDev;
            return this;
        }

        public Builder vehicleStrategy(VehicleGenerationStrategyEnum strategy) {
            this.vehicleStrategy = strategy;
            return this;
        }

        public Builder planStrategy(PlanGenerationStrategyEnum strategy) {
            this.planStrategy = strategy;
            return this;
        }

        public Builder hubStrategy(HubGenerationStrategyEnum strategy) {
            this.hubStrategy = strategy;
            return this;
        }

        public Builder publisherRateMs(Long publisherRateMs){
            this.publisherRateMs = publisherRateMs;
            return this;
        }

        public Builder publisherDirty(boolean publisherDirty){
            this.publisherDirty = publisherDirty;
            return this;
        }

        public Builder realTime(boolean realTime){
            this.realTime = realTime;
            return this;
        }

        public ConfigRun build() {

            /* ===== Validazioni base ===== */
            if (configPath == null || configPath.isBlank()) {
                throw new IllegalStateException("Config path must be set");
            }

            if (numeroVeicoli == null || numeroVeicoli <= 0) {
                throw new IllegalStateException("Numero veicoli must be > 0");
            }

            validateSoc("socMedio", socMedio);
            validateSoc("targetSocMean", targetSocMean);

            if (socStdDev != null && socStdDev < 0) {
                throw new IllegalStateException("socStdDev must be >= 0");
            }

            if (targetSocStdDev != null && targetSocStdDev < 0) {
                throw new IllegalStateException("targetSocStdDev must be >= 0");
            }

            /* ===== Validazione strategie ===== */
            if (vehicleStrategy == null) {
                throw new IllegalStateException("VehicleGenerationStrategy must be set");
            }

            if (planStrategy == null) {
                throw new IllegalStateException("PlanGenerationStrategy must be set");
            }

            /* ===== Validazione risorse in base alla strategia ===== */
            if (vehicleStrategy == VehicleGenerationStrategyEnum.FROM_CSV && csvResourceEv == null) {
                throw new IllegalStateException("csvResourceEv required for FROM_CSV vehicle strategy");
            }

            if (hubStrategy == HubGenerationStrategyEnum.FROM_CSV && csvResourceHub == null) {
                throw new IllegalStateException("csvResourceHub required for FROM_CSV hub strategy");
            }

            return new ConfigRun(this);
        }

        private void validateSoc(String field, Double value) {
            if (value == null || value < 0.0 || value > 1.0) {
                throw new IllegalStateException(field + " must be in range [0,1]");
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
