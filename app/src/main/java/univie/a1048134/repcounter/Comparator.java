package univie.a1048134.repcounter;

import org.opencv.features2d.DMatch;

/**
 * Created by klexx on 12.06.2015.
 */
public class Comparator implements java.util.Comparator<DMatch>{
    @Override
    public int compare(DMatch dMatch, DMatch  t1) {
        float result =  dMatch.distance - t1.distance;
        return Math.round(result);
    }
}
