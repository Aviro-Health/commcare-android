package org.commcare.android.resource.installers;

import org.commcare.resources.model.Resource;
import org.commcare.resources.model.ResourceLocation;
import org.commcare.resources.model.ResourceTable;
import org.commcare.resources.model.UnresolvedResourceException;
import org.commcare.utils.AndroidCommCarePlatform;
import org.commcare.utils.FileUtil;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xml.util.UnfullfilledRequirementsException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import androidx.core.util.Pair;

/**
 * @author ctsims
 */
public class MediaFileAndroidInstaller extends FileSystemInstaller {

    private String path;

    @SuppressWarnings("unused")
    public MediaFileAndroidInstaller() {
        // For externalization
    }

    public MediaFileAndroidInstaller(String destination, String upgradeDestination, String path) {
        super(destination + (path == null ? "" : "/" + path), upgradeDestination + (path == null ? "" : "/" + path));
        //establish whether dir structure needs to be extended?
        this.path = path;
    }

    @Override
    public boolean install(Resource r, ResourceLocation location, Reference ref, ResourceTable table, AndroidCommCarePlatform platform, boolean upgrade, boolean recovery) throws UnresolvedResourceException, UnfullfilledRequirementsException {

        // If it's a lazy media resource and we are not lazy recovering resource,
        // just add the resource to the table without actually installing it.
        if (r.isLazy() && !recovery) {
            resolveEmptyLocalReference(r, location, upgrade);
            table.commit(r, upgrade ? Resource.RESOURCE_STATUS_UPGRADE : Resource.RESOURCE_STATUS_INSTALLED);
            return true;
        }


        return super.install(r, location, ref, table, platform, upgrade, recovery);
    }

    @Override
    public boolean upgrade(Resource r, AndroidCommCarePlatform platform) {
        if (r.isLazy()) {
            return true;
        }

        return super.upgrade(r, platform);
    }

    @Override
    public boolean uninstall(Resource r, AndroidCommCarePlatform platform) throws UnresolvedResourceException {
        boolean success = super.uninstall(r, platform);
        if (!success) {
            return false;
        }
        //cleanup dirs
        return FileUtil.cleanFilePath(this.localDestination, path);
    }

    @Override
    protected int customInstall(Resource r, Reference local, boolean upgrade, AndroidCommCarePlatform platform) {
        return upgrade ? Resource.RESOURCE_STATUS_UPGRADE : Resource.RESOURCE_STATUS_INSTALLED;
    }

    @Override
    public boolean requiresRuntimeInitialization() {
        return false;
    }

    @Override
    public boolean initialize(AndroidCommCarePlatform platform, boolean isUpgrade) {
        return false;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        super.readExternal(in, pf);
        path = ExtUtil.nullIfEmpty(ExtUtil.readString(in));
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        super.writeExternal(out);
        ExtUtil.writeString(out, ExtUtil.emptyIfNull(path));
    }

    @Override
    public Pair<String, String> getResourceName(Resource r, ResourceLocation loc) {
        int index = loc.getLocation().lastIndexOf("/");
        if (index == -1) {
            return new Pair<>(loc.getLocation(), ".dat");
        }
        String fileName = loc.getLocation().substring(index);

        String extension = ".dat";
        int lastDot = fileName.lastIndexOf(".");
        if (lastDot != -1) {
            extension = fileName.substring(lastDot);
            fileName = fileName.substring(0, lastDot);
        }
        return new Pair<>(fileName, extension);
    }
}
