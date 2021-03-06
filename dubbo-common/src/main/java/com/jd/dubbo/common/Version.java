package com.jd.dubbo.common;


import com.jd.dubbo.common.logger.Logger;
import com.jd.dubbo.common.logger.LoggerFactory;

import java.security.CodeSource;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * Description: <br>
 *
 * @author <a href=mailto:lianle1@jd.com>连乐</a>
 * @date 2016/1/21 11:26
 */
public final class Version {

    private Version() {}

    private static final Logger logger = LoggerFactory.getLogger(Version.class);

    private static final String VERSION = getVersion(Version.class, "2.0.0");

    private static final boolean INTERNAL = hasResource("com/jd/dubbo/registry/internal/RemoteRegistry.class");

    private static final boolean COMPATIBLE = hasResource("com/taobao/remoting/impl/connectionRequest.class");

    static {
        //检查是否存在重复的jar包
        Version.checkDuplicate(Version.class);
    }

    public static String getVersion(){return VERSION;}

    public static boolean isInternalVersion() {
        return INTERNAL;
    }

    public static boolean isCompatibleVersion(){return COMPATIBLE;}

    public static boolean hasResource(String path) {
        try {
            return Version.class.getClassLoader().getResource(path) != null;
        }catch (Throwable t) {
            return false;
        }
    }

    public static String getVersion(Class<?> cls, String defaultVersion) {
        try {
            //首先检查MANIFEST.MF规范中的版本号
            String version = cls.getPackage().getImplementationVersion();
            if (version == null || version.length() == 0) {
                version = cls.getPackage().getSpecificationVersion();
            }
            if (version == null || version.length() == 0) {
                //如果规范中没有版本号，基于jar包名获取版本号
                CodeSource codeSource = cls.getProtectionDomain().getCodeSource();
                if (codeSource == null) {
                    logger.info("No codeSource for class " + cls.getName() + " when getVersion, use default version " + defaultVersion);
                } else {
                    String file = codeSource.getLocation().getFile();
                    if (file != null && file.length() > 0 && file.endsWith(".jar")) {
                        file = file.substring(0, file.length() - 4);
                        int i = file.lastIndexOf('/');
                        if (i >= 0) {
                            file = file.substring(i + 1);
                        }
                        i = file.indexOf("-");
                        if (i >= 0) {
                            file = file.substring(i + 1);
                        }

                        while (file.length() > 0 && !Character.isDigit((file.charAt(0)))) {
                            i = file.indexOf("-");
                            if (i >= 0) {
                                file = file.substring(i + 1);
                            } else {
                                break;
                            }
                        }
                        version = file;
                    }
                }
            }
            //返回版本号，如果为空返回缺省版本号
            return version == null || version.length() == 0 ? defaultVersion : version;
        } catch (Throwable t) {
            //忽略异常，返回缺省版本号
            logger.error("return default version, ignore exception " + t.getMessage(), t);
            return defaultVersion;
        }
    }

    public static void checkDuplicate(Class<?> cls, boolean failOneError) {
        checkDuplicate(cls.getName().replace('.', '/') + ".class", failOneError);
    }

    public static void checkDuplicate(Class<?> cls) {
        checkDuplicate(cls, false);
    }

    public static void checkDuplicate(String path, boolean failOnError) {
        try {
            //在ClassPath搜文件
            Enumeration<URL> urls = ClassHelper.getCallerClassLoader(Version.class).getResources(path);

            Set<String> files = new HashSet<String>();
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url != null) {
                    String file = url.getFile();
                    if (file != null && file.length() > 0) {
                        files.add(file);
                    }
                }
            }
            //如果有多个，就表示重复
            if (files.size() > 1) {
                String error = "Duplicate class " + path + " in " + files.size() + " jar" + files;
                if (failOnError) {
                    throw new IllegalStateException(error);
                }else {
                    logger.error(error);
                }
            }

        }catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
    }
}
