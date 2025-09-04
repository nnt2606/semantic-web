package org.example.model.asianCountry;

public class CountryFact {
    private String country;
    private String capital;
    private String capitalUri;
    private String countryUri;
    private String capitalLabel;
    private Long population;
    private String thumbnail;


    public CountryFact(String countryUri, String countryName,
                       String capitalUri, String capitalName,
                       Long population, String thumbnail) {
        this.countryUri = countryUri;
        this.country = countryName;
        this.capitalUri = capitalUri;
        this.capital = capitalName;
        this.population = population;
        this.thumbnail = thumbnail;
    }


    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCapital() {
        return capital;
    }

    public void setCapital(String capital) {
        this.capital = capital;
    }

    public String getCapitalLabel() {
        return capitalLabel;
    }

    public void setCapitalLabel(String capitalLabel) {
        this.capitalLabel = capitalLabel;
    }

    public Long getPopulation() {
        return population;
    }

    public void setPopulation(Long population) {
        this.population = population;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getCapitalUri() {
        return capitalUri;
    }

    public void setCapitalUri(String capitalUri) {
        this.capitalUri = capitalUri;
    }

    public String getCountryUri() {
        return countryUri;
    }

    public void setCountryUri(String countryUri) {
        this.countryUri = countryUri;
    }

    @Override
    public String toString() {
        return "CountryFact{" +
                "countryName='" + country + '\'' +
                ", capitalName='" + capital + '\'' +
                ", population=" + population +
                '}';
    }

}
