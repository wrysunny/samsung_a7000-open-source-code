package com.sec.android.app.fm;

import com.samsung.android.feature.FloatingFeature;
import com.sec.android.app.CscFeature;
import com.sec.android.app.CscFeatureTagFMRadio;
import com.sec.android.app.SecProductFeature_FMRADIO;

public class FMRadioFeature {
    public static CscFeature sCscFeature = CscFeature.getInstance();
    public final static int NARROW_WIDTH_SCANSPACE = 50;
    public static final String BANDWIDTHAS_87500_108000 = "87500_108000";
    public static final String BANDWIDTHAS_76000_108000 = "76000_108000";
    public static final String BANDWIDTHAS_76000_90000 = "76000_90000";
    public static final String CHIP_SILICON = "1";
    public static final String CHIP_BRAODCOM = "2";
    public static final String CHIP_MRVL = "3";
    public static final String CHIP_QCOM = "4";
    public static final String CHIP_RICHWAVE = "5";
    public static final String CHIP_SPRD = "6";

    private static int FEATURE_FREQUENCYSPACE = sCscFeature
            .getInteger(CscFeatureTagFMRadio.TAG_CSCFEATURE_FMRADIO_FREQUENCYSPACEAS);

    public static final String FEATURE_DEFAULTCHANNEL = sCscFeature
            .getString(CscFeatureTagFMRadio.TAG_CSCFEATURE_FMRADIO_DEFAULTCHANNELAS);

    public static boolean FEATURE_DISABLEMENUAF = sCscFeature
            .getEnableStatus(CscFeatureTagFMRadio.TAG_CSCFEATURE_FMRADIO_DISABLEMENUAF);

    public static final boolean FEATURE_DISABLEMENURDS = (sCscFeature
            .getEnableStatus(CscFeatureTagFMRadio.TAG_CSCFEATURE_FMRADIO_DISABLEMENURDS))
            || !(SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_SUPPORT_RDS);

    public static final String FEATURE_BANDWIDTH = sCscFeature
            .getString(CscFeatureTagFMRadio.TAG_CSCFEATURE_FMRADIO_BANDWIDTHAS);

    public static final int FEATURE_DECONSTANT = sCscFeature
            .getInteger(CscFeatureTagFMRadio.TAG_CSCFEATURE_FMRADIO_DECONSTANTAS);

    public static final int FEATURE_RECORDINGVOLUME = sCscFeature
            .getInteger(CscFeatureTagFMRadio.TAG_CSCFEATURE_FMRADIO_RECORDINGVOLUMEAS);

    public static final boolean FEATURE_DISABLERTPLUSINFO = sCscFeature
            .getEnableStatus(CscFeatureTagFMRadio.TAG_CSCFEATURE_FMRADIO_DISABLERTPLUSINFO);

    public static final boolean FEATURE_DISABLEDNS = !((SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_SUPPORT_HYBRIDRADIO) && !(sCscFeature
            .getEnableStatus(CscFeatureTagFMRadio.TAG_CSCFEATURE_FMRADIO_DISABLEMENUINTERNETRADIO)));
    public static final boolean EPG_PI_IMAGE_CACHE = true;
    
    public static final boolean FEATURE_ENABLE_SURVEY_MODE = FloatingFeature.getInstance().getEnableStatus("SEC_FLOATING_FEATURE_CONTEXTSERVICE_ENABLE_SURVEY_MODE");

    static {
        if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_REMOVE_AF_MENU)
            FEATURE_DISABLEMENUAF = true;
    }

    public static void ForceApply_ThailandFunction() {
        FEATURE_FREQUENCYSPACE = 50;
    }

    public static void Recovery_ThailandFunction() {
        FEATURE_FREQUENCYSPACE = sCscFeature
                .getInteger(CscFeatureTagFMRadio.TAG_CSCFEATURE_FMRADIO_FREQUENCYSPACEAS);
    }

    public static int GetFrequencySpace() {
        return FEATURE_FREQUENCYSPACE;
    }

    public static String GetBandWidthAs() {
        return FEATURE_BANDWIDTH;
    }

    public static int GetDeconstantAs() {
        return FEATURE_DECONSTANT;
    }

}
