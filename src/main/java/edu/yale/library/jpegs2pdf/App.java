package edu.yale.library.jpegs2pdf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

public class App {

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			throw new IOException("You must provide the json file and output file as arguments.");
		}
		JpegPdfConcat pdfConcat = new JpegPdfConcat();
		JsonReader jsonReader = Json.createReader(new FileReader(args[0]));
		JsonObject document = jsonReader.readObject();
		jsonReader.close();
		JsonArray pages = document.getJsonArray("pages");
		if ( document.getBoolean("displayCoverPage", false) ) {
			JsonArray properties = document.getJsonArray("properties");
			Map<String, String> documentProperties = mapFromJsonArray(properties);
			pdfConcat.setupCoverPage(document.getString("title"), documentProperties, document.getString("logoImage"), document.getString("logoText"));
		}
		List<File> tempFiles = new ArrayList<File>();
		for (JsonValue pageValue : pages) {
			JsonObject page = (JsonObject) pageValue;
			Map<String, String> pageProperties = mapFromJsonArray(page.getJsonArray("properties"));
			String filename = page.getString("file");
			if (filename.startsWith("http://") || filename.startsWith("https://")) {
				File destination = File.createTempFile("pdf-gen", ".jpg");
				URL website = new URL(filename);
				ReadableByteChannel rbc = Channels.newChannel(website.openStream());
				FileOutputStream fos = new FileOutputStream(destination);
				try {
					fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				} finally {
					fos.close();
				}
				tempFiles.add(destination);
				filename = destination.getAbsolutePath();
			}
			pdfConcat.addJpegPage(new File(filename), page.getString("caption"), pageProperties);
		}
		pdfConcat.generatePdf(new File(args[1]));
		for (File tempFile : tempFiles) {
			tempFile.delete();
		}
	}

	private static Map<String, String> mapFromJsonArray(JsonArray properties) throws IOException {
		LinkedHashMap<String, String> ret = new LinkedHashMap<String, String>();
		if (properties != null) {
			for (JsonValue propertyValue : properties) {
				JsonObject property = (JsonObject) propertyValue;
				ret.put(property.getString("name"), property.getString("value"));
			}
		}
		return ret;
	}
}
