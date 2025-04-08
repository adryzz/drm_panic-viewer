package gay.nihil.lena.drm_panic_viewer;

import android.net.Uri;

public class PanicMessage {
    public String log;
    public String reason;
    public String hardwareName;
    public String architecture;
    public String version;
    public String distribution;
    public Uri reportUri;

    public PanicMessage(Uri uri) {
        if (uri.getHost() == null) {
            // raw kernel panic
            this.log = uri.toString();

            log.lines().forEach(l -> {
                // TODO: make this faster and abstract out
                if (l.contains("Kernel panic - not syncing:")) {
                    this.reason = l.substring(l.indexOf(':') + 2);
                }

                if (l.contains("Hardware name:")) {
                    this.hardwareName = l.substring(l.indexOf(':') + 2);
                }
            });
        } else {
            // TODO: fix
            this.log = log;
            this.architecture = architecture;
            this.version = version;
            this.reportUri = uri;

            if (distribution != null) {
                this.distribution = distribution;
            } else {
                // TODO: make a list of names
                this.distribution = reportUri.getHost();
            }

            log.lines().forEach(l -> {
                // TODO: make this faster and abstract out
                if (l.contains("Kernel panic - not syncing:")) {
                    this.reason = l.substring(l.indexOf(':') + 2);
                }

                if (l.contains("Hardware name:")) {
                    this.hardwareName = l.substring(l.indexOf(':') + 2);
                }
            });
        }
    }
}