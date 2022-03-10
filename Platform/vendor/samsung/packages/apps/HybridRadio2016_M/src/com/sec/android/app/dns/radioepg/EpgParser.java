package com.sec.android.app.dns.radioepg;

public abstract class EpgParser {
    private class XmlParserThread extends Thread {
        private String mKey = null;

        private XmlParserThread(String key) {
            mKey = key;
        }

        private String getKey() {
            return mKey;
        }

        public void run() {
            String freq = getKey();
            EpgData data = getData(freq);
            if (data == null || ready(data)) {
                EpgParser.this.notify(data);
                return;
            }
            parse(data);
        }
    }

    protected abstract EpgData getData(final String freq);

    protected abstract void notify(EpgData data);

    protected abstract void parse(EpgData data);

    protected abstract boolean ready(final EpgData data);

    public final void run(String freq) {
        Thread t = new XmlParserThread(freq);
        t.start();
    }
}