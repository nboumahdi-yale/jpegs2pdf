package edu.yale.library.jpegs2pdf;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class JpegPdfConcat {

	private List<JpegPdfPage> pages = new ArrayList<JpegPdfPage>();
	private Map<String, String> properties;
	private String caption;
	private String logoImageFile;
	private String logoText;

	public void generatePdf(File destinationFile) throws IOException {
		long start = System.currentTimeMillis();
		PDDocument document = new PDDocument();
		if (this.properties != null) {
			addCoverPageToPDDocument(document);
		}
		for (JpegPdfPage page : pages) {
			page.addToPDDocument(document);
		}
		document.save(destinationFile);
		document.close();
		long time = System.currentTimeMillis() - start;
		System.out.println("Generated: " + pages.size() + " in " + time + " at " + (time / pages.size()));
	}

	public void setupCoverPage(String caption, Map<String, String> properties, String logoImageFile,
			String logoText) {
		this.properties = properties;
		this.caption = caption;
		this.logoImageFile = logoImageFile;
		this.logoText = logoText;
	}

	public void addJpegPage(File jpegFile, String caption) {
		addJpegPage(jpegFile, caption, null);
	}

	public void addJpegPage(File jpegFile, String caption, Map<String, String> properties) {
		JpegPdfPage page = new JpegPdfPage();
		page.jpegFile = jpegFile;
		page.caption = caption;
		page.properties = properties;
		pages.add(page);
	}
	
	private void addCoverPageToPDDocument(PDDocument document) throws IOException {
		PDPage page = new PDPage(PDRectangle.LETTER);
		document.addPage(page);
		PDPageContentStream contentStream = new PDPageContentStream(document, page, AppendMode.OVERWRITE, false);

		if (logoImageFile != null) {
			BufferedImage bimg = null;
			InputStream in = new FileInputStream(logoImageFile);
			try {
				bimg = ImageIO.read(in);
			} finally {
				in.close();
			}
			int margin = 50;
			float width = bimg.getWidth();
			float height = bimg.getHeight();
			float imageAspect = width / height;
			float x, y, w, h;
			x = margin;
			h = 50;
			y = PDRectangle.LETTER.getHeight() - margin - h;
			w = h * imageAspect;
			PDImageXObject pdImageXObject = LosslessFactory.createFromImage(document, bimg);
			contentStream.drawImage(pdImageXObject, x, y, w, h);
			if (logoText != null) {
				contentStream.beginText();
				int fontSize = 14;
				PDFont font = PDType1Font.HELVETICA_BOLD;
				contentStream.setFont(font, fontSize);

				contentStream.newLineAtOffset(margin + w + 10, y + 10);
				contentStream.showText(logoText);
				contentStream.endText();
			}

		}

		drawPropertiesToContentStream(contentStream, caption, properties, 20, 10, 10, 150);
		contentStream.close();
	}

	private float drawWithWidth(PDPageContentStream content, String text, float paragraphWidth, PDFont font, int fontSize)
			throws IOException {
		;
		int start = 0;
		int end = 0;
		float fontHeight = (font.getFontDescriptor().getCapHeight()) / 1000 * fontSize * (float) 1.5;
		float height = 0;
		for (int i : possibleWrapPoints(text)) {
			float width = font.getStringWidth(text.substring(start, i)) / 1000 * fontSize;
			if (start < end && width > paragraphWidth) {
				content.showText(text.substring(start, end));
				content.newLineAtOffset(0, -fontHeight);
				height += fontHeight;
				start = end;
			}
			end = i;
		}
		content.showText(text.substring(start));
		return height;
	}

	private int[] possibleWrapPoints(String text) {
		String[] split = text.split("(?<=\\W)");
		int[] ret = new int[split.length];
		ret[0] = split[0].length();
		for (int i = 1; i < split.length; i++)
			ret[i] = ret[i - 1] + split[i].length();
		return ret;
	}

	private float drawPropertiesToContentStream(PDPageContentStream contentStream, String caption,
			Map<String, String> properties,  int titleFontSize, int fontSize, float yPos) throws IOException {
		return drawPropertiesToContentStream(contentStream, caption, properties, titleFontSize, fontSize, yPos, 50);
	}

	private float drawPropertiesToContentStream(PDPageContentStream contentStream, String caption,
			Map<String, String> properties, int titleFontSize, int fontSize, float yPos, float topMargin) throws IOException {
		float margin = 50;
		PDFont font = PDType1Font.HELVETICA_BOLD;
		PDFont font2 = PDType1Font.HELVETICA;
		float titleFontHeight = (font.getFontDescriptor().getCapHeight()) / 1000 * titleFontSize;
		float fontHeight = (font.getFontDescriptor().getCapHeight()) / 1000 * fontSize;
		contentStream.beginText();
		contentStream.setFont(font, titleFontSize);
		float titleLeading = titleFontHeight * 2;
		float leading = fontHeight * 2;
		contentStream.setLeading(titleLeading);
		contentStream.newLineAtOffset(margin, PDRectangle.LETTER.getHeight() - topMargin - fontHeight);
		yPos += drawWithWidth(contentStream, caption, PDRectangle.LETTER.getWidth() - 2 * margin, font, titleFontSize);
		contentStream.newLine();
		contentStream.setFont(font, fontSize);
		contentStream.setLeading(leading);
		yPos += leading;
		if (properties != null) {
			float maxTitleWidth = 0;
			for (Map.Entry<String, String> entry : properties.entrySet()) {
				maxTitleWidth = Math.max(font.getStringWidth(entry.getKey()) / 1000 * fontSize + 10, maxTitleWidth);
			}
			float paraWidth = PDRectangle.LETTER.getWidth() - 2 * margin - maxTitleWidth;
			for (Map.Entry<String, String> entry : properties.entrySet()) {
				contentStream.setFont(font, fontSize);
				contentStream.showText(entry.getKey());
				contentStream.newLineAtOffset(maxTitleWidth, 0);
				contentStream.setFont(font2, fontSize);
				yPos += drawWithWidth(contentStream, entry.getValue(), paraWidth, font, fontSize);
				// contentStream.drawString( entry.getValue() );
				contentStream.newLineAtOffset(-maxTitleWidth, -fontHeight * (float) 2.2);
				yPos += fontHeight * (float) 2.2;

			}
		}

		contentStream.endText();
		return yPos;

	}


	private class JpegPdfPage {
		File jpegFile;
		String caption;
		Map<String, String> properties;

		void addToPDDocument(PDDocument document) throws IOException {
			InputStream in = new FileInputStream(jpegFile);
			try {
				float margin = 50;
				PDPage page = new PDPage(PDRectangle.LETTER);
				document.addPage(page);
				PDPageContentStream contentStream = new PDPageContentStream(document, page, AppendMode.OVERWRITE, true);
				float yPos = drawPropertiesToContentStream(contentStream, caption, properties, 12, 9, 10);
				BufferedImage bimg = ImageIO.read(in);
				float width = bimg.getWidth();
				float height = bimg.getHeight();
				float pageAspect = PDRectangle.LETTER.getWidth() / (PDRectangle.LETTER.getHeight() - yPos);
				float imageAspect = width / height;
				float x, y, w, h;
				if (pageAspect > imageAspect) {
					y = margin;
					h = PDRectangle.LETTER.getHeight() - yPos - 2 * margin;
					w = h * imageAspect;
					x = (PDRectangle.LETTER.getWidth() - w) / 2;
				} else {
					x = margin;
					w = PDRectangle.LETTER.getWidth() - 2 * margin;
					h = w / imageAspect;
					y = (PDRectangle.LETTER.getHeight() - yPos - h) / 2;
				}

				PDImageXObject pdImageXObject = LosslessFactory.createFromImage(document, bimg);
				contentStream.drawImage(pdImageXObject, x, y, w, h);

				contentStream.close();

			} finally {
				in.close();
			}

		}
	}

}
