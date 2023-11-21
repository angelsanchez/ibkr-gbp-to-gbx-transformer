import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Main {
	private static final String[] NODE_NAMES = { "Order", "Trade", "SymbolSummary", "CashTransaction" };
	private static final String[] ATTR_NAMES = { "tradePrice", "tradeMoney", "netCash", "cost", "closePrice",
			"ibCommission", "amount" };

	public static void main(String[] args)
			throws ParserConfigurationException, SAXException, IOException, TransformerException {

		if (args.length < 1) {
			System.out.println(
					"Error: Missing args.\nUsage: java Main [inputFile]");
			return;
		}

		String inputFilePath = args[0];
		String outputFilePath = args[0].replaceAll(".xml", "").concat("-transformed.xml");

		File inputFile = new File(inputFilePath);
		File outputFile = new File(outputFilePath);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		Document document = builder.parse(inputFile);
		Element root = document.getDocumentElement();

		for (String nodeName : NODE_NAMES) {
			processNode(root, nodeName);
		}

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(document);

		StreamResult result = new StreamResult(outputFile);
		transformer.transform(source, result);

		System.out.println("Output file generated at: " + outputFile.getAbsolutePath());
	}

	private static void processNode(Element root, String tagName) {
		NodeList nodes = root.getElementsByTagName(tagName);

		for (int i = 0; i < nodes.getLength(); i++) {
			Node order = nodes.item(i);
			NamedNodeMap attributes = order.getAttributes();
			Node currency = attributes.getNamedItem("currency");

			if ("GBP".equals(currency.getNodeValue())) {
				currency.setNodeValue("GBX");
				Node commissionCurrency = attributes.getNamedItem("ibCommissionCurrency");
				if (commissionCurrency != null) {
					commissionCurrency.setNodeValue("GBX");
				}

				for (String nodeName : ATTR_NAMES) {
					toGBX(attributes, nodeName);
				}

				toGBXRate(attributes, "fxRateToBase");
			}
		}
	}

	private static void toGBX(NamedNodeMap attributes, String attrName) {
		transformNode(attributes, attrName, TransformOperation.AMOUNT);
	}

	private static void toGBXRate(NamedNodeMap attributes, String attrName) {
		transformNode(attributes, attrName, TransformOperation.FXRATE);
	}

	private static void transformNode(NamedNodeMap attributes, String attrName, TransformOperation operation) {
		Node node = attributes.getNamedItem(attrName);
		if (node == null) {
			return;
		}

		String nodeValue = node.getNodeValue();
		if ("".equals(nodeValue)) {
			return;
		}

		float value = Float.parseFloat(nodeValue);
		float transformedValue;
		switch (operation) {
		case AMOUNT:
			transformedValue = value * 100;
			break;
		case FXRATE:
			transformedValue = value / 100;
			break;
		default:
			throw new IllegalArgumentException("Operation " + operation + " not supported");
		}

		node.setNodeValue(Float.toString(transformedValue));
	}
}
