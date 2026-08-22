package rt.data.embedder;

public class VectorSimilarity {

    public static double cosine(float[] a, float[] b) {

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

    public static double norm(float[] a) {
        double sum = 0;

        for (float x : a) {
            sum += x * x;
        }

        return Math.sqrt(sum);
    }
}