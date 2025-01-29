package ocgcore;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.WorkerThread;

import com.file.zip.ZipEntry;
import com.file.zip.ZipFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipInputStream;

import cn.garymb.ygomobile.App;
import cn.garymb.ygomobile.AppsSettings;
import cn.garymb.ygomobile.Constants;
import cn.garymb.ygomobile.utils.IOUtils;
import ocgcore.data.Card;


public class CardManager {
    private static int cdbNum = 0;
    private final SparseArray<Card> cardDataHashMap = new SparseArray<>();
    private final String dbDir;
    private final String exDbPath;
    private static final String TAG = String.valueOf(CardManager.class);

    /**
     * @see DataManager#getCardManager()
     * @param dbDir
     * @param exPath
     */
    CardManager(String dbDir, String exPath) {
        this.dbDir = dbDir;
        this.exDbPath = exPath;
    }

    private static SQLiteDatabase openDatabase(String file) {
        return SQLiteDatabase.openDatabase(file, null,
                SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
    }

    public static boolean checkDataBase(File file) {
        if (!file.exists()) {
            return false;
        }
        Cursor reader = null;
        SQLiteDatabase db = null;
        boolean rs = false;
        try {
            db = openDatabase(file.getPath());
            reader = db.rawQuery("select datas.id, ot, alias, setcode, type, level, race, attribute, atk, def,category,name,\"desc\" from datas,texts  where datas.id = texts.id limit 1;", null);
            rs = reader != null;
        } catch (Throwable e) {
            //ignore
        } finally {
            IOUtils.close(reader);
        }
        if (!rs) {
            try {
                reader = db.rawQuery("select datas._id, ot, alias, setcode, type, level, race, attribute, atk, def,category,name,\"desc\" from datas,texts where datas._id = texts._id  limit 1;", null);
                rs = reader != null;
            } catch (Throwable e) {
                //ignore
            } finally {
                IOUtils.close(reader);
            }
        }
        IOUtils.close(db);
        return rs;
    }

    public static List<File> readZipCdb(String zipPath) throws IOException {
        String savePath = App.get().getExternalCacheDir().getAbsolutePath();
        List<File> fileList = new ArrayList<>();

        ZipFile zf = new ZipFile(zipPath, "GBK");
        InputStream in = new BufferedInputStream(new FileInputStream(zipPath));
        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry ze;
        Enumeration<ZipEntry> entris = zf.getEntries();
        while (entris.hasMoreElements()) {
            ze = entris.nextElement();
            if (ze.isDirectory()) {
                //Do nothing
            } else {
                if (ze.getName().endsWith(".cdb")) {
                    File file = new File(savePath, "cards" + cdbNum + ".cdb");
                    InputStream inputStream = zf.getInputStream(ze);
                    OutputStream os = new FileOutputStream(file);
                    int bytesRead = 0;
                    byte[] buffer = new byte[8192];
                    while ((bytesRead = inputStream.read(buffer, 0, 8192)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    os.close();
                    inputStream.close();
                    fileList.add(file);
                    cdbNum++;
                }
            }
        }
        zin.closeEntry();
        return fileList;
    }

    public Card getCard(int code) {
        Card card = cardDataHashMap.get(code);
        if (card == null) {
            card = new Card(code);
            cardDataHashMap.put(code, new Card(code));
            return card;
        }
        return card;
    }

    public int getCount() {
        return cardDataHashMap.size();
    }

    public SparseArray<Card> getAllCards() {
        return cardDataHashMap;
    }

    /**
     * 清空cardDataHashMap，之后从cdb文件读取卡牌，到cardDataHashMap
     * 如果开启了先行卡，
     */
    @WorkerThread
    public void loadCards() {
        cardDataHashMap.clear();
        int count = readAllCards(AppsSettings.get().getDatabaseFile(), cardDataHashMap);
        Log.i(TAG, "load defualt cdb:" + count);
        if (!TextUtils.isEmpty(exDbPath)) {
            if (AppsSettings.get().isReadExpansions()) {
                File dir = new File(exDbPath);
                if (dir.exists()) {
                    File[] files = dir.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            File file = new File(dir, name);
                            return file.isFile() && ((name.endsWith(".cdb") || (name.endsWith(".zip") || name.endsWith(Constants.YPK_FILE_EX))));
                        }
                    });
                    //读取全部卡片
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().endsWith(".cdb")) {
                                count = readAllCards(file, cardDataHashMap);
                            } else if (file.getName().endsWith(".zip") || file.getName().endsWith(Constants.YPK_FILE_EX)) {
                                Log.e("CardManager", "读取压缩包");
                                try {
                                    for (File file1 : readZipCdb(file.getAbsolutePath())) {
                                        count = readAllCards(file1, cardDataHashMap);
                                    }
                                } catch (IOException e) {
                                    Log.e("CardManager", "读取压缩包错误" + e);
                                }
                            }
                            Log.i(TAG, "load " + count + " cdb:" + file);
                        }
                    }
                }
            }
        }
        buildAliasCards();
    }

    private void buildAliasCards() {
        int N = getCount();
        for (int i = 0; i < N; i++) {
            Card c = cardDataHashMap.valueAt(i);
            if (c.Alias == 0) {
                continue;
            }
            //规则同名，或者多图同名
            Card alias = getCard(c.Alias);
            if (alias != null) {
                if (c.isSame(alias)) {
                    //多图同名，它们属性必定是一致
                    c.setRealCode(alias.Code);
                } else if (Math.abs(c.Alias - c.Code) <= 10) {
                    Log.w(TAG, c.Name + ":" + c.Code + " is same card " + c.Alias);
                }
            }
        }
    }

    @WorkerThread
    protected int readAllCards(File file, SparseArray<Card> cardMap) {
        if (!file.exists()) {
            return 0;
        }
        int i = 0;
        Cursor reader = null;
        SQLiteDatabase db = null;
        try {
            db = openDatabase(file.getPath());
            try {
                reader = db.rawQuery("select datas.id, ot, alias, setcode, type, level, race, attribute, atk, def,category,name,\"desc\" from datas,texts where datas.id = texts.id;", null);
            } catch (Throwable e) {
                //ignore
                reader = db.rawQuery("select datas._id, ot, alias, setcode, type, level, race, attribute, atk, def,category,name,\"desc\" from datas,texts where datas._id = texts._id;", null);
            }
            if (reader != null && reader.moveToFirst()) {
                do {
                    Card cardData = new Card();
                    cardData.Code = reader.getInt(0);
                    cardData.Ot = reader.getInt(1);
                    cardData.Alias = reader.getInt(2);
                    cardData.SetCode = reader.getLong(3);
                    cardData.Type = reader.getLong(4);
                    int levelInfo = reader.getInt(5);
                    cardData.Level = levelInfo & 0xff;
                    cardData.LeftScale = (levelInfo >> 24) & 0xff;
                    cardData.RightScale = (levelInfo >> 16) & 0xff;
                    cardData.Race = reader.getLong(6);
                    cardData.Attribute = reader.getInt(7);
                    cardData.Attack = reader.getInt(8);
                    cardData.Defense = reader.getInt(9);
                    cardData.Category = reader.getLong(10);
                    cardData.Name = reader.getString(11);
                    cardData.Desc = reader.getString(12);
                    //put
                    i++;
                    cardMap.put(cardData.Code, cardData);
                } while (reader.moveToNext());
            }
        } catch (Throwable e) {
            e.printStackTrace();
            Log.e(TAG, "read cards " + file, e);
        } finally {
            IOUtils.close(reader);
            IOUtils.close(db);
        }
        return i;
    }
}
