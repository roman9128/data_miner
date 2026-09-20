package rt.data_processing.embedder;

import java.nio.ByteBuffer;

public final class VectorUtils {

    public static double cosineSimilarity(float[] a, float[] b) {

        double dot = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static byte[] floatArrayToByteArray(float[] floats) {
        if (floats == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4);
        buffer.asFloatBuffer().put(floats);
        return buffer.array();
    }

    public static float[] byteArrayToFloatArray(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] floats = new float[bytes.length / 4];
        buffer.asFloatBuffer().get(floats);
        return floats;
    }
}