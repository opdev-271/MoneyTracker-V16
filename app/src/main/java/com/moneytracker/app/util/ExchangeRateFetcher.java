package com.moneytracker.app.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Fetches the European Central Bank's daily reference rates - free, keyless, and
 * official, already covering every currency this app supports. Uses only
 * java.net/javax.xml (no new Gradle dependency, same reasoning as the xlsx export's
 * hand-rolled writer). Rates are expressed as units of that currency per 1 EUR, the
 * feed's implicit base.
 */
public class ExchangeRateFetcher {

    private static final String ECB_DAILY_RATES_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";
    private static final int TIMEOUT_MILLIS = 8000;

    private ExchangeRateFetcher() {
    }

    /** Blocking network call - must run off the main thread. Throws on any failure. */
    public static Map<String, Double> fetchRatesRelativeToEur() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(ECB_DAILY_RATES_URL).openConnection();
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setRequestMethod("GET");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Unexpected HTTP status " + status + " from exchange rate feed");
            }
            try (InputStream in = connection.getInputStream()) {
                return parseRates(in);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static Map<String, Double> parseRates(InputStream in) throws IOException {
        Map<String, Double> rates = new HashMap<>();
        rates.put("EUR", 1.0);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            // Defensive against XXE even though the source is a fixed, trusted HTTPS
            // endpoint - if this feature isn't recognized by the platform's parser,
            // fall back to parsing without it rather than failing the whole fetch.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (ParserConfigurationException ignored) {
            // Unsupported on this parser; proceed anyway.
        }

        Document document;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(in));
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException("Could not parse exchange rate feed", e);
        }

        NodeList cubes = document.getElementsByTagName("Cube");
        for (int i = 0; i < cubes.getLength(); i++) {
            Element cube = (Element) cubes.item(i);
            String currency = cube.getAttribute("currency");
            String rateText = cube.getAttribute("rate");
            if (currency.isEmpty() || rateText.isEmpty()) {
                continue;
            }
            try {
                rates.put(currency, Double.parseDouble(rateText));
            } catch (NumberFormatException ignored) {
                // skip malformed entry
            }
        }

        if (rates.size() <= 1) {
            throw new IOException("Exchange rate feed returned no usable rates");
        }
        return rates;
    }
}
