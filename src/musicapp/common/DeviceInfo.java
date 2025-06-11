package musicapp.common;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;
import musicapp.utils.MD5;

public class DeviceInfo {

    public static String getIMEI() {
        String out = "";

        try {
            out = System.getProperty("com.imei");
            if (out == null || out.equals("null") || out.equals("")) {
                out = System.getProperty("phone.imei");
            }

            if (out == null || out.equals("null") || out.equals("")) {
                out = System.getProperty("com.nokia.IMEI");
            }

            if (out == null || out.equals("null") || out.equals("")) {
                out = System.getProperty("com.nokia.mid.imei");
            }

            if (out == null || out.equals("null") || out.equals("")) {
                out = System.getProperty("com.sonyericsson.imei");
            }

            if (out == null || out.equals("null") || out.equals("")) {
                out = System.getProperty("IMEI");
            }

            if (out == null || out.equals("null") || out.equals("")) {
                out = System.getProperty("com.motorola.IMEI");
            }

            if (out == null || out.equals("null") || out.equals("")) {
                out = System.getProperty("com.samsung.imei");
            }

            if (out == null || out.equals("null") || out.equals("")) {
                out = System.getProperty("com.siemens.imei");
            }

            if (out == null || out.equals("null") || out.equals("")) {
                out = System.getProperty("imei");
            }
        } catch (Exception var2) {
            return out == null ? "" : out;
        }

        return out == null ? "" : out;
    }

    public static String getIMSI() {
        String out = "";

        try {
            out = System.getProperty("IMSI");
            if (out == null || out.equals("null") || out.equals("")) {
                out = System.getProperty("phone.imsi");
            }

            if (out == null || out.equals("null") || out.equals("")) {
                out = System.getProperty("com.nokia.mid.mobinfo.IMSI");
            }

            if (out == null || out.equals("null") || out.equals("")) {
                out = System.getProperty("com.nokia.mid.imsi");
            }

            if (out == null || out.equals("null") || out.equals("")) {
                out = System.getProperty("IMSI");
            }

            if (out == null || out.equals("null") || out.equals("")) {
                out = System.getProperty("imsi");
            }
        } catch (Exception var2) {
            return out == null ? "" : out;
        }

        return out == null ? "" : out;
    }

    private static String getNokiaImei() {
        if (System.getProperty("phone.imei") != null) {
            return System.getProperty("phone.imei");
        } else {
            return System.getProperty("com.nokia.imei") != null ? System.getProperty("com.nokia.imei") : System.getProperty("com.nokia.mid.imei");
        }
    }

    private static String getSonyEricssonImei() {
        return System.getProperty("com.sonyericsson.imei");
    }

    private static String getSamsungImei() {
        return System.getProperty("com.samsung.imei");
    }

    private static String getMotorolaImei() {
        return System.getProperty("IMEI") != null ? System.getProperty("IMEI") : System.getProperty("com.motorola.IMEI");
    }

    private static String getSiemensImei() {
        return System.getProperty("com.siemens.IMEI");
    }

    private static String getLGImei() {
        return System.getProperty("com.lge.imei");
    }

    public static String getDeviceImei() {
        String imei = getNokiaImei();
        if (imei != null && imei.length() > 0) {
            return imei;
        } else {
            imei = getLGImei();
            if (imei != null && imei.length() > 0) {
                return imei;
            } else {
                imei = getMotorolaImei();
                if (imei != null && imei.length() > 0) {
                    return imei;
                } else {
                    imei = getSiemensImei();
                    if (imei != null && imei.length() > 0) {
                        return imei;
                    } else {
                        imei = getSamsungImei();
                        if (imei != null && imei.length() > 0) {
                            return imei;
                        } else {
                            imei = getSonyEricssonImei();
                            if (imei != null && imei.length() > 0) {
                                return imei;
                            } else {
                                imei = getIMEI();
                                if (imei == null || imei.equals("null") || imei.equals("")) {
                                    imei = getIMSI();
                                }

                                if (imei == null || imei.equals("null") || imei.equals("")) {
                                    RecordStore rs = null;
                                    boolean existingOrNot = existing("aRS");
                                    if (existingOrNot) {
                                        try {
                                            rs = RecordStore.openRecordStore("aRS", false);
                                            byte[] byteInputData = new byte[1];
                                            int length = 0;

                                            for (int x = 1; x <= rs.getNumRecords(); ++x) {
                                                if (rs.getRecordSize(x) > byteInputData.length) {
                                                    byteInputData = new byte[rs.getRecordSize(x)];
                                                }

                                                length = rs.getRecord(x, byteInputData, 0);
                                            }

                                            imei = new String(byteInputData, 0, length);
                                            rs.closeRecordStore();
                                            System.out.println("Get IMEI EXISTED " + imei);
                                        } catch (Exception var21) {
                                        } finally {
                                            ;
                                        }
                                    } else {
                                        try {
                                            rs = RecordStore.openRecordStore("aRS", true);
                                        } catch (Exception var19) {
                                            System.out.println(var19.getMessage());
                                        } finally {
                                            ;
                                        }

                                        long fadeIMEI = System.currentTimeMillis();
                                        imei = String.valueOf(fadeIMEI);
                                        imei = MD5.asHex(imei.getBytes());

                                        try {
                                            rs.addRecord(imei.getBytes(), 0, imei.length());
                                            rs.closeRecordStore();
                                        } catch (RecordStoreException var18) {
                                            System.out.println(var18.getMessage());
                                        }

                                        System.out.println("Get IMEI NO EXISTED " + imei);
                                    }
                                }

                                return imei;
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean existing(String recordStoreName) {
        boolean existingOrNot = false;
        RecordStore rs = null;
        if (recordStoreName.length() > 32) {
            return false;
        } else {
            try {
                rs = RecordStore.openRecordStore(recordStoreName, false);
                existingOrNot = true;
            } catch (RecordStoreNotFoundException var14) {
                existingOrNot = false;
            } catch (Exception var15) {
            } finally {
                try {
                    rs.closeRecordStore();
                } catch (Exception var13) {
                }

            }

            return existingOrNot;
        }
    }
}
