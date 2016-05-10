
import subprocess 
import xml.etree.ElementTree as ET

#Apps are 5-tuples consisting of (app_id, domain, build_number, username, password)
APPS_LIST = [
	("a370e321169d2555a86d3e174f3024c2", "aliza-test", 53, "t1", "123"), 
	("b82a236700f293976e2290aaeae778a1", "aliza-test", 17, "t1", "123"), 
	("73d5f08b9d55fe48602906a89672c214", "aliza-test", 49, "t1", "123")
]

RELATIVE_PATH_TO_ASSETS_DIR = "./app/standalone/assets"


def download_ccz(app_id, domain, build_number):
	#TODO: Get HQ to implement downloading a specific build
	subprocess.call(["./scripts/download_app_into_standalone_asset.sh", domain, app_id, RELATIVE_PATH_TO_ASSETS_DIR]) 


def download_restore_file(domain, username, password):
	subprocess.call(["./scripts/download_restore_into_standalone_asset.sh", domain, username, password, RELATIVE_PATH_TO_ASSETS_DIR])


def assemble_apk(domain, build_number):
	subprocess.call(["gradle", "assembleStandaloneDebug", 
		"-Pcc_domain={}".format(domain), 
		"-Papplication_name={}".format(get_app_name_from_profile()), 
		"-Pis_consumer_app=true", 
		"-PversionCode={}".format(build_number), "--stacktrace"])


def get_app_name_from_profile():
	tree = ET.parse(RELATIVE_PATH_TO_ASSETS_DIR + '/direct_install/profile.ccpr')
	return tree.getroot().get("name")


def move_apk(app_id):
	subprocess.call(["mkdir", "-p", "./build/outputs/consumer_apks"]) 
	subprocess.call(["mv", "./build/outputs/apk/commcare-odk-standalone-debug.apk", "./build/outputs/consumer_apks/{}.apk".format(app_id)])


for (app_id, domain, build_number, username, password) in APPS_LIST:
	download_ccz(app_id, domain, build_number)
	download_restore_file(domain, username, password)
	assemble_apk(domain, build_number)
	move_apk(app_id)


