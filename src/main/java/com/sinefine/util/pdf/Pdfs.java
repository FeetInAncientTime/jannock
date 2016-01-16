package com.sinefine.util.pdf;

import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

/**
 * A utility class providing methods used to compare
 * <a href="http://www.adobe.com/products/acrobat/adobepdf.html">PDF</a>
 * documents.
 *
 * <p>
 * Often, a simple byte by byte comparison cannot be performed on a generated PDF file and the
 * expected PDF file as both files contain information that is specific to the exact environment in
 * which they were created and the time at which they were created. Hence, this class provides more
 * lenient methods of comparison.
 * </p>
 *
 * <p>
 * This class is thread-safe.</p>
 *
 * <p>
 * TODO Consider adding a method which returns a diff list. TODO Consider adding a main method in
 * order to enable the application to be used from the command line.</p>
 */
public final class Pdfs {

  /**
   * The Logger class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(Pdfs.class);

  private static String DEFAULT_CHARSET = "ISO-8859-1";

  /**
   * The {@code Configuration} class represents the configuration to be used by methods of the
   * class.
   *
   * <p>
   * The configuration includes the lines (normal and array) to ignore in PDF document.
   * </p>
   * <p>
   * This class is immutable and therefore thread-safe.</p>
   */
  static final class Configuration {

    /**
     * The set of line prefixes (array types) to ignore within the PDF file.
     */
    private final Set<String> arrayLinePrefixesToIgnore;

    /**
     * The set of line prefixes (non array types) to ignore within the PDF file.
     */
    private final Set<String> linePrefixesToIgnore;

    /**
     * Initializes a new instance of the Configuration class.
     *
     * <p>
     * Although null values are authorized in the sets {@code arrayLinePrefixesToIgnore} and
     * {@code linePrefixesToIgnore} they are not taken into account.</p>
     *
     * @param arrayLinePrefixesToIgnore the set of line prefixes (array types) to ignore within the
     *     PDF file.
     * @param linePrefixesToIgnore the set of line prefixes (non array types) to ignore within the
     *     PDF file.
     * @throws NullPointerException if either of the arguments are null.
     */
    public Configuration(final Set<String> arrayLinePrefixesToIgnore,
        final Set<String> linePrefixesToIgnore) {
      this.arrayLinePrefixesToIgnore
          = Collections.unmodifiableSet(
              removeNulls(arrayLinePrefixesToIgnore));
      this.linePrefixesToIgnore
          = Collections.unmodifiableSet(
              removeNulls(linePrefixesToIgnore));
    }

    /**
     * Return a new instance of the set set in which the null values have been removed.
     *
     * @param set the set.
     * @return a new instance of the set set in which the null values have been removed.
     */
    private Set<String> removeNulls(final Set<String> set) {
      return set.parallelStream().filter(p -> p != null)
          .collect(Collectors.toSet());
    }

    /**
     * Returns the set of line prefixes (for array types) to be ignored with the PDF file.
     *
     * <p>
     * The returned set is immutable.</p>
     *
     * @return the set of line prefixes (for array types) to be ignored with the PDF file.
     */
    public Set<String> getArrayLinePrefixesToIgnore() {
      return arrayLinePrefixesToIgnore;
    }

    /**
     * Returns the set of line prefixes (non array types) to ignore within the PDF file.
     *
     * <p>
     * The returned set is immutable.</p>
     *
     * @return the set of line prefixes (non array types) to ignore within the PDF file.
     */
    public Set<String> getLinePrefixesToIgnore() {
      return linePrefixesToIgnore;
    }

  }

  /**
   * The default line prefixes to ignore.
   */
  private static final Set<String> DEFAULT_LINE_PREFIXES_TO_IGNORE;

  static {
    final Set<String> s = new HashSet<>();
    Collections.addAll(s,
        "/Producer", "/Creator", "/CreationDate", "/DocChecksum",
        "/Root",
        //The << symbols indicate the start of a dictionary object.
        "<</Producer", "<</Creator", "<</CreationDate",
        "<</DocChecksum", "<</Root",
        // Commented lines
        "%"
    );
    DEFAULT_LINE_PREFIXES_TO_IGNORE = Collections.unmodifiableSet(s);
  }

  /**
   * The set of default (array) line prefixes to ignore.
   */
  private static final Set<String> DEFAULT_ARRAY_LINE_PREFIXES_TO_IGNORE;

  static {
    final Set<String> s = new HashSet<>();
    Collections.addAll(s, "/ID", "<</ID");
    DEFAULT_ARRAY_LINE_PREFIXES_TO_IGNORE = Collections.unmodifiableSet(s);
  }

  /**
   * An immutable instance of the default configuration.
   */
  static final Configuration DEFAULT_CONFIGURATION
      = new Configuration(DEFAULT_ARRAY_LINE_PREFIXES_TO_IGNORE,
          DEFAULT_LINE_PREFIXES_TO_IGNORE);

  private Pdfs() {
    throw new AssertionError("The class "
        + Pdfs.class.getCanonicalName()
        + " is not intended to be instatiated!");
  }

  /**
   * Returns {@code true} if the two PDF InputStreams are equal, {@code false} otherwise.
   *
   * <p>
   * This method is equivalent to:</p>
   * <ul>
   * <li>{@linkplain #areEqual(byte[], byte[])}, where the byte arrays represent the input
   * streams.</li>
   * </ul>
   *
   * @param actual The first PDF input stream
   * @param expected The second PDF input stream
   * @return {@code true} if the two PDF InputStreams are equal, {@code false} otherwise.
   * @throws IOException if an error occurs whilst processing the input streams.
   */
  public static boolean areEqual(final InputStream actual,
      final InputStream expected) throws IOException {
    return areEqual(
        IOUtils.toByteArray(actual), IOUtils.toByteArray(expected));
  }

  /**
   * Returns {@code true} if the two PDF byte arrays are equal, {@code false} otherwise.
   *
   * <p>
   * Two PDF documents are considered equal if either of the following methods returns
   * {@code true}.</p>
   * <ul>
   * <li>{@linkplain #areContentsEqual(byte[], byte[])}</li>
   * <li>{@linkplain #areImagesSame(byte[], byte[])}</li>
   * </ul>
   *
   * <p>
   * For performance reasons, the method {@linkplain
   * Pdfs#areContentsEqual(byte[], byte[])} <em>is always evaluated before</em> the method null
   * {@link #areImagesSame(InputStream, InputStream)}.
   * </p>
   *
   * @param actual The first PDF byte array
   * @param expected The second PDF byte array
   * @return {@code true} if the two PDF byte arrays are equal, {@code false} otherwise.
   * @throws IOException if an error occurs whilst processing the byte arrays.
   * @see #areEqual(InputStream, InputStream)
   */
  public static boolean areEqual(final byte[] actual,
      final byte[] expected) throws IOException {
    return areContentsEqual(actual, expected)
        || areImagesSame(actual, expected);
  }

  /**
   * Returns {@code true} if the contents of the two PDF InputStreams are equal, {@code false}
   * otherwise.
   *
   * <p>
   * A simple byte by byte comparison cannot be performed on the generated PDF file and the expected
   * PDF file as both files contain information that is specific to the exact environment in which
   * they were created and the time at which they were created.
   * </p>
   *
   * <p>
   * This method attempts to avoid these problems by ignoring lines that start with the following
   * environment specific tags:
   * </p>
   * <ul>
   * <li>/Producer</li>
   * <li>/Creator</li>
   * <li>/CreationDate</li>
   * <li>/DocChecksum</li>
   * <li>/Root</li>
   * </ul>
   *
   * <p>
   * If an item in the above list appears as a dictionary object at the start of a line then it is
   * also ignored. Dictionary objects are identified by the use of the {@literal <<} symbols.
   * </p>
   *
   * <p>
   * Commented lines (i.e. lines that begin with the text '%') are also ignored. If the comment is
   * <em>not</em> the very first character of the line then the line <em>will</em> be included in
   * the test.
   * </p>
   *
   * <p>
   * This method will also ignore lines that start with the following tags:
   * </p>
   * <ul>
   * <li>/ID</li>
   * <li>{@literal <<}/ID</li>
   * </ul>
   * <p>
   * In the case of the above tags, subsequent lines will also be ignored until a line contains the
   * symbol ']'. After that subsequent lines will be processed as before.
   * </p>
   * <p>
   * Please note that two {@code null} values are <em>not</em> equal.
   * </p>
   *
   * @param actual The first PDF input stream
   * @param expected The second PDF input stream
   * @return {@code true} if the contents of two input streams are equal, {@code false} otherwise.
   * @throws java.io.IOException if an error occurs whilst reading the input streams.
   */
  public static boolean areContentsEqual(final InputStream actual,
      final InputStream expected) throws IOException {
    return areContentsEqual(actual, expected,
        DEFAULT_CONFIGURATION);
  }

  /**
   * Returns {@code true} if the contents of the two PDF InputStreams are equal, {@code false}
   * {@literal otherwise.} Unlike {@link #areContentsEqual}, this method allows clients to specify
   * the configuration.
   *
   * @param actual The first PDF input stream
   * @param expected The second PDF input stream
   * @param configuration the configuration to use.
   * @return {@code true} if the contents of the two PDF InputStreams are equal, {@code false}
   * {@literal otherwise.}
   * @throws IOException if an error occurs whilst reading the input streams.
   */
  static boolean areContentsEqual(final InputStream actual,
      final InputStream expected, final Configuration configuration)
      throws IOException {
    if (actual == null || expected == null) {
      return false;
    } else {
      final Set<String> linePrefixesToIgnore = configuration
          .getLinePrefixesToIgnore();
      final Set<String> arrayLinePrefixesToIgnore = configuration
          .getArrayLinePrefixesToIgnore();
      try (final BufferedReader b1 = new BufferedReader(new InputStreamReader(actual,
          DEFAULT_CHARSET));
          final BufferedReader b2 = new BufferedReader(new InputStreamReader(expected,
                  DEFAULT_CHARSET))) {
        // Create a buffered reader using the default charset.
        // As long as the two input files use the same charset
        // then this should cause few problems.
        String line1;
        String line2;
        int lineNumber = 1;
        boolean isInIgnoredArray = false;
        while ((line1 = b1.readLine()) != null) {
          line2 = b2.readLine();
          if (line2 == null) {
            return false;
          } else {
            if (!line1.equals(line2)) {
              if (isInIgnoredArray) {
                isInIgnoredArray = !(line1.contains("]"));
              } else {
                if (skipLine(line1, line2, arrayLinePrefixesToIgnore)) {
                  isInIgnoredArray = true;
                } else if (!skipLine(line1, line2, linePrefixesToIgnore)) {
                  LOGGER.error("The following lines [#" + lineNumber + "] are different!\r\n\t"
                      + "1. " + line1 + "\r\n\t" + "2. " + line2);
                  return false;
                }
              }
            }
          }
          lineNumber++;
        }
        return true;
      }
    }
  }

  /**
   * Returns {@code true} if the contents of the two PDF byte arrays are equal, {@code false}
   * otherwise.
   *
   * <p>
   * This method is equivalent to: </p>
   * <blockquote>
   * <code>areEqual(
   * new ByteArrayInputStream(actual), new ByteArrayInputStream(expected))
   * </code>
   * </blockquote>
   *
   * @param actual The first PDF byte array
   * @param expected The second PDF byte array
   * @return {@code true} if the contents of the two PDF byte arrays are equal, {@code false}
   *     otherwise.
   * @throws IOException if an error occurs whilst processing the byte arrays.
   * @see #areEqual(InputStream, InputStream)
   */
  public static boolean areContentsEqual(final byte[] actual,
      final byte[] expected) throws IOException {
    return areContentsEqual(
        new ByteArrayInputStream(actual), new ByteArrayInputStream(
            expected));
  }

  /**
   * Returns {@code true} if the difference in size between the actual byte array and the expected
   * byte array is less than the tolerated difference.
   *
   * <p>
   * Visually identical PDF files may not have exactly the same size as the generated PDF file and
   * the expected PDF file may contain information that is specific to the exact environment in
   * which they were created and the time at which they were created.
   * </p>
   *
   * <p>
   * The tolerance is expressed as a value between 0 and 1 inclusive. A zero tolerance value
   * indicates that the arrays must be the same size. The maximum tolerance value of 1 indicates
   * that the size of the actual byte array can be any value between 1 and twice the size of the
   * expected byte array.</p>
   *
   * <p>
   * Please note that two {@code null} values are <em>not</em> equal.</p>
   *
   * @param actual the actual byte array.
   * @param expected the expected byte array.
   * @param tolerance the tolerance.
   * @return {@code true} if the difference in size between the actual byte array and the expected
   *     byte array is less than the tolerated difference.
   * @throws IllegalArgumentException if the tolerance is less than zero or greater than one.
   */
  public static boolean areContentsSimilarSize(final byte[] actual,
      final byte[] expected, final float tolerance) {
    if (tolerance < 0 || tolerance > 1) {
      throw new IllegalArgumentException(
          "The tolerance must be between 0 and 1!");
    }
    if (actual == null || expected == null) {
      return false;
    } else {
      final int actualSize = actual.length;
      final int expectedSize = expected.length;
      //If the difference in size between the actual file and the
      //expected file is more than the tolerated proportion
      //then return false
      return expectedSize > ((1.0 - tolerance) * actualSize)
          && expectedSize < ((1.0 + tolerance) * actualSize);
    }
  }

  /**
   * Returns {@code true} if the difference in size between the actual input stream and the expected
   * input stream is less than the tolerated difference.
   *
   * <p>
   * This method is equivalent to the method {@link #areContentsSimilarSize(byte[], byte[], float)}
   * where the input streams are converted to byte arrays.
   * </p>
   *
   * @param actual the actual input stream.
   * @param expected the expected input stream.
   * @param tolerance the tolerance.
   * @return {@code true} if the difference in size between the actual input stream and the expected
   *     input stream is less than the tolerated difference.
   * @throws IOException if an I/O error occurs whilst trying to read the input streams.
   * @see #areContentsSimilarSize(byte[], byte[], float)
   */
  public static boolean areContentsSimilarSize(final InputStream actual,
      final InputStream expected, final float tolerance) throws
      IOException {
    if (tolerance < 0 || tolerance > 1) {
      throw new IllegalArgumentException(
          "The tolerance (" + tolerance
          + ") must be between 0 and 1!");
    }
    if (actual == null || expected == null) {
      return false;
    } else {
      return areContentsSimilarSize(
          IOUtils.toByteArray(actual), IOUtils.toByteArray(expected),
          tolerance);
    }
  }

  /**
   * Returns {@code true} if the <em>images</em> of the two PDF input streams are the same,
   * {@code false} otherwise.
   *
   * <p>
   * This method is equivalent to {@link #areImagesSame(byte[], byte[])} where the input streams are
   * converted to byte arrays.
   * </p>
   *
   * @param actual The first PDF input stream.
   * @param expected The second PDF input stream.
   * @return {@code true} if the contents of the two PDF byte arrays are equal, {@code false}
   *     otherwise.
   * @throws IOException if an error occurs whilst processing the input streams.
   */
  public static boolean areImagesSame(final InputStream actual,
      final InputStream expected) throws IOException {
    if (actual == null || expected == null) {
      return false;
    } else {
      return areImagesSame(
          IOUtils.toByteArray(actual), IOUtils.toByteArray(expected));
    }
  }

  /**
   * Returns {@code true} if the <em>images</em> of the two PDF byte arrays are the same,
   * {@code false} otherwise.
   *
   * <p>
   * Any differences between the documents that do not change the rendered image are ignored.</p>
   *
   * @param actual the actual byte array.
   * @param expected the expected byte array.
   * @return {@code true} if the two PDF <em>images</em> are the same, {@code false} otherwise.
   * @throws IOException if an error occurs whilst processing the byte arrays.
   */
  public static boolean areImagesSame(final byte[] actual,
      final byte[] expected) throws IOException {
    PDDocument actualPdfDocument = null;
    PDDocument expectedPdfDocument = null;
    try (final InputStream actualInputStream = new ByteArrayInputStream(
        actual);
        final InputStream expectedInputStream
        = new ByteArrayInputStream(
            expected)) {
      actualPdfDocument = PDDocument.load(actualInputStream);
      expectedPdfDocument = PDDocument.load(expectedInputStream);
      final List<byte[]> actualPages = toPageImages(actualPdfDocument);
      final List<byte[]> expectedPages = toPageImages(expectedPdfDocument);
      return areByteListsEqual(actualPages, expectedPages);
    } finally {
      closeQuietly(actualPdfDocument);
      closeQuietly(expectedPdfDocument);
    }
  }

  /**
   * Returns {@code true}, if the list of byte arrays are equal, {@code false} otherwise.
   *
   * <p>
   * Lists are considered equal, if:</p>
   * <ul>
   * <li>both lists are {@code null},</li>
   * <li>or both lists contain exactly the same bytes in the same order.</li>
   * </ul>
   *
   * @param listX the first list.
   * @param listY the second list.
   * @return {@code true}, if the list of byte arrays are equal, {@code false} otherwise.
   */
  private static boolean areByteListsEqual(final List<byte[]> listX,
      final List<byte[]> listY) {
    if (listX == null && listY == null) {
      return true;
    } else if (listX != null && listY != null) {
      if (listX.size() == listY.size()) {
        boolean areEqual = true;
        for (int i = 0, len = listX.size(); i < len; i++) {
          if (!Arrays.equals(listX.get(i), listY.get(i))) {
            areEqual = false;
            break;
          }
        }
        return areEqual;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  /**
   * Returns a list of byte arrays representing the pages of the PDF document as images.
   *
   * @param pdfDocument the PDF document.
   * @return a list of byte arrays representing the pages of the PDF document as images.
   * @throws IOException if an I/O error occurs during the reading of the document or the creation
   *     of the page images.
   */
  private static List<byte[]> toPageImages(final PDDocument pdfDocument)
      throws IOException {
    @SuppressWarnings("unchecked")
    final List<PDPage> pages = pdfDocument.getDocumentCatalog()
        .getAllPages();
    final List<byte[]> pageImages = new ArrayList<>();
    for (PDPage page : pages) {
      try (final ByteArrayOutputStream byteArrayOutputStream
          = new ByteArrayOutputStream()) {
        final BufferedImage image = page.convertToImage();
        boolean hasSucceeded = ImageIO.write(image, "png",
            byteArrayOutputStream);
        if (hasSucceeded) {
          pageImages.add(byteArrayOutputStream.toByteArray());
        }
      }
    }
    return pageImages;
  }

  /**
   * Closes the instance of the PDDocument without throwing an exception.
   *
   * @param pdDocument an instance of the class {@linkplain PDDocument}.
   */
  private static void closeQuietly(final PDDocument pdDocument) {
    if (pdDocument != null) {
      try {
        pdDocument.close();
      } catch (IOException ioe) {
        //ignore exception
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("An exception occured whilst attempting "
              + "to close an instance of the PDDocument class.  "
              + "Although this exception will not cause the "
              + "application to fail, it should be investigated.",
              ioe);
        }
      }
    }
  }

  /**
   * Returns {@code true} if the lines are to be skipped, {@code false} otherwise.
   *
   * @param line1 the line from the first document.
   * @param line2 the line from the second document.
   * @param linePrefixesToIgnore a set of line prefixes to ignore.
   * @return {@code true} if the lines are to be skipped, {@code false} otherwise.
   */
  private static boolean skipLine(final String line1, final String line2,
      final Set<String> linePrefixesToIgnore) {
    return linePrefixesToIgnore.parallelStream().anyMatch(
        prefix -> line1.startsWith(prefix) && line2.startsWith(prefix));
  }

}
