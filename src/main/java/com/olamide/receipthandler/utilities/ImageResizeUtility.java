package com.olamide.receipthandler.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Resizes receipt images before they are sent to the AI provider so fewer
 * image tokens are consumed. Uses only JDK classes ({@code java.awt.image}
 * / {@code ImageIO}) — no OpenCV, no ImageMagick, no Spring dependency.
 *
 * <p>Key design decisions (Raspberry-Pi-friendly):
 * <ul>
 *   <li>Max long-edge is 1600 px — big enough for receipt text to stay
 *       legible, small enough to cut token usage by 60-80 % on typical
 *       phone-camera photos (4000 × 3000).</li>
 *   <li>Output is always JPEG at ~87 % quality, even if the source was
 *       PNG/WEBP. JPEG is the most compact lossy format that both Gemini
 *       and Claude handle well.</li>
 *   <li>PDFs are returned unchanged — rasterising PDFs is a separate
 *       concern and would pull in heavy libraries.</li>
 *   <li>If anything goes wrong (unreadable format, I/O error), the
 *       original bytes are returned unchanged so the upload still
 *       succeeds — it just costs more tokens.</li>
 * </ul>
 */
public final class ImageResizeUtility {

    private static final Logger log = LoggerFactory.getLogger(ImageResizeUtility.class);

    /** Maximum number of pixels on the longest edge before resizing kicks in. */
    static final int MAX_LONG_EDGE = 1600;

    /** JPEG compression quality (0.0 – 1.0). 0.87 preserves receipt text
     *  legibility while keeping file size well under the original. */
    static final float JPEG_QUALITY = 0.87f;

    private static final String JPEG_FORMAT = "jpg";
    private static final String JPEG_MIME = "image/jpeg";

    private ImageResizeUtility() {}

    /**
     * The result of a resize operation: the (possibly resized) bytes and
     * the correct MIME type to send to the AI provider.  When no resize
     * happens the MIME type is the original; when we re-encode to JPEG it
     * is always {@value #JPEG_MIME}.
     */
    public record ResizeResult(byte[] bytes, String mimeType) {}

    /**
     * Resize the given image bytes if the long edge exceeds {@value #MAX_LONG_EDGE}.
     *
     * @param fileBytes raw file content (JPEG, PNG, WEBP, or PDF)
     * @param mimeType  MIME type of the file
     * @return a {@link ResizeResult} with the (possibly resized) bytes and
     *         the correct MIME type for downstream AI callers
     */
    public static ResizeResult resizeIfNeeded(byte[] fileBytes, String mimeType) {
        if (fileBytes == null || fileBytes.length == 0 || isPdf(mimeType)) {
            return new ResizeResult(fileBytes, mimeType);
        }

        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(fileBytes));
            if (original == null) {
                // ImageIO couldn't decode the format — return as-is.
                return new ResizeResult(fileBytes, mimeType);
            }

            int width = original.getWidth();
            int height = original.getHeight();
            int longEdge = Math.max(width, height);

            if (longEdge <= MAX_LONG_EDGE) {
                log.debug("Image {}x{} is already within {} px limit — skipping resize",
                        width, height, MAX_LONG_EDGE);
                return new ResizeResult(fileBytes, mimeType);
            }

            double scale = (double) MAX_LONG_EDGE / longEdge;
            int newWidth  = (int) Math.round(width  * scale);
            int newHeight = (int) Math.round(height * scale);

            BufferedImage resized = resizeImage(original, newWidth, newHeight);
            // Let GC reclaim the large decoded bitmap as soon as possible.
            original = null;

            byte[] resizedBytes = toJpeg(resized);

            double savedPct = 100.0 * (1.0 - (double) resizedBytes.length / fileBytes.length);
            log.info("Resized receipt image: {}x{} ({} bytes) → {}x{} ({} bytes) [saved {}%%]",
                    width,  height,  fileBytes.length,
                    newWidth, newHeight, resizedBytes.length,
                    String.format("%.1f", savedPct));

            return new ResizeResult(resizedBytes, JPEG_MIME);

        } catch (IOException e) {
            // Resize failed — fall back to the original bytes so the
            // upload is not blocked. The AI call will just cost more tokens.
            log.warn("Image resize failed, sending original bytes: {}", e.getMessage());
            return new ResizeResult(fileBytes, mimeType);
        }
    }

    // -------------------------------------------------------------- private

    private static BufferedImage resizeImage(BufferedImage source, int w, int h) {
        // TYPE_INT_RGB drops any alpha channel, which JPEG doesn't support.
        BufferedImage target = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static byte[] toJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(
                image.getWidth() * image.getHeight() / 4); // rough estimate

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(JPEG_FORMAT);
        if (!writers.hasNext()) {
            throw new IOException("No JPEG ImageWriter available");
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);

            writer.setOutput(new MemoryCacheImageOutputStream(baos));
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    private static boolean isPdf(String mimeType) {
        return mimeType != null && mimeType.equals("application/pdf");
    }
}
