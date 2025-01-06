import cv2
import numpy as np
import os
import convert.Perspec2Equirec as P2E
import time
import random, math

height,width=512, 1024

# samsung
# verticalFOV = 55.1
# horizontalFOV = 69.6

# vivo
verticalFOV = 57.8
horizontalFOV = 72.8

view_W = round(width * verticalFOV / 360)
view_H = round(view_W * 4 / 3)

phi = []
theta = []

def get_result(data_path):
    # print(os.path.exists(data_path))
    # assert os.access(data_path, os.R_OK), f"File not readable: {data_path}"

    # 1
    # start_time=time.time()
    pano_path=compose_photos(data_path)
    # print("compose time: "+str(round(time.time()-start_time,3)))

    # pano_path=data_path

    # 2
    start_time=time.time()
    image=cv2.imread(pano_path, cv2.IMREAD_GRAYSCALE)
    # assert image is not None, "Image could not be loaded"
    downsample_image = cv2.resize(image, None, fx=0.25, fy=0.25)
    height,width=image.shape[0], image.shape[1]
    center_x, center_y=findEmptyArea(downsample_image)
    print("find time: "+str(round(time.time()-start_time,3)))
    if center_x != -1:
        phi = ((center_x*4 * 360 // width) + 180) % 360
        theta = center_y*4 * 180 // height - 90
        return phi,theta
    return -1,-1

def compose_photos(data_path):
    global phi, theta

    resizedImgW = round(width * verticalFOV / 360)
    resizedImgH = round(resizedImgW * 4 / 3)

    files = os.listdir(data_path)  # 列出目標資料夾中的所有檔案
    # print(files)

    jpg_count = 0
    for file in files:
        if file.endswith(".jpg") and "_" not in file:
            jpg_count += 1
    # print(f"有 {jpg_count} 個.jpg格式的input images")

    file_path = data_path + "/Orientation.txt"
    with open(file_path, "r") as file:
        lines = file.readlines()


    for line in lines:
        newLine = line.strip()  # 使用strip()方法刪除每行的換行符號
        coordinate = newLine.split(" ")
        phi.append(int(coordinate[0]))
        theta.append(int(coordinate[1]))

    imgs = []
    # paste images to compositeImage
    for i in range(jpg_count):
        filename = "img" + str(i)
        img = cv2.imread(data_path + "/" + filename + ".jpg")
        img = cv2.resize(img, (resizedImgW, resizedImgH))
        imgs.append(img)

    pano_path=data_path + "/inp.png"
    cv2.imwrite(pano_path, persToEqui(imgs))
    phi = []
    theta = []
    # print("finish compose photos.")
    return pano_path

def get_dirList():
    dirs=[
        [20, 70, 110, 160, 200, 250, 290, 340],
        [25, 70, 110, 155, 205, 250, 290, 335],
        [30, 70, 110, 150, 210, 250, 290, 330]
    ]
    rand=random.randrange(len(dirs))
    return dirs[rand]

def weight(y, height):
    if y < height/2:
        return 1 + 6.8*(height/2 - y)//height

    return 1 + 4.8*(y - height/2)//height

def findEmptyArea(image):
    height,width=image.shape[0], image.shape[1]

    dist=[]
    coord=[]
    threshold=37

    # for y in range(0, height):
    #     for x in range(0, width):
    #         dir=get_dirList()
    #         dt=[]
    #         if image[y,x] ==0:
    #             isBigArea=True
    #             for i in range(len(dir)):
    #                 valid, d=linearSearch(image, x, y, dir[i])
    #                 if d is None: continue
    #
    #                 d = d / weight(y, height)
    #
    #                 if valid and isShortRange(d, threshold):
    #                     isBigArea=False
    #                     break
    #
    #                 dt.append(d)
    #
    #             if isBigArea:
    #                 min1=min(dt)
    #                 dist.append(min1)
    #                 coord.append((x,y))
    if len(dist)==0:
        return (-1,-1)

    return coord[dist.index(max(dist))]

def persToEqui(img_array):
    merge_image = np.zeros((height, width, 3))
    merge_mask = np.zeros((height, width, 3))

    for img, T, P in zip(img_array, theta, phi):
        per = P2E.Perspective(
            img, verticalFOV, horizontalFOV, T, P
        )  # Load equirectangular image
        img, mask = per.GetEquirec(
            height, width
        )  # Specify parameters(FOV, theta, phi, height, width)
        merge_image += img
        merge_mask += mask

    # cv2.imwrite(data_path+"\mask.png", np.where(merge_mask == 0, 0, 1))


    merge_mask = np.where(merge_mask == 0, 1, merge_mask)
    merge_image = np.divide(merge_image, merge_mask)
    # cv2.imshow("masked image", merge_image)
    # cv2.waitKey(0)
    # cv2.destroyAllWindows()

    return merge_image

def linearSearch(image, start_x, start_y, direction):
    # start_x, start_y = x, y
    start_phi, start_theta=x_to_phi(image, start_x), y_to_theta(image, start_y)
    h, w = image.shape
    radian = math.radians(direction)
    i=1
    while True:
        x=round(start_x + i*math.sin(radian))
        y=round(start_y + i*math.cos(radian))

        if x<0 or x>=image.shape[1]:
            x = (x+w)%w

        if  y<0 or y>=h:
            if abs(x_to_phi(image, x) - start_phi) >180:
                return False, None
            return False, haversine_distance(start_phi, start_theta, x_to_phi(image, x), y_to_theta(image, y), 50)

        if  image[y,x] != 0:
            return True, haversine_distance(start_phi, start_theta, x_to_phi(image, x), y_to_theta(image, y), 50)

        i+=8

def enoughDistance(image, x, y, direction):
    dist=0
    while True:
        x+=direction[0]
        y+=direction[1]
        dist+=1
        if dist>=30:
            return True
        if  y<0 or y>=image.shape[0] or image[y,x] != 0:
            return False

        if x<0 or x>=image.shape[1]:
            x = (x+image.shape[1])%image.shape[1]

def isShortRange(d):
    if d<36:
        return True
    return False

def x_to_phi(img, x):
    return (x  / img.shape[1]) *360 - 180

def y_to_theta(img, y):
    return (y / img.shape[0]) * 180 - 90

def haversine_distance(phi1, theta1, phi2, theta2, radius=6371):
    phi1, theta1, phi2, theta2 = np.radians([phi1, theta1, phi2, theta2])

    cos_d = math.sin(theta1) * math.sin(theta2) + math.cos(theta1) * math.cos(theta2) * math.cos(phi1 - phi2)

    # 防止浮點誤差導致的cos_d超過 [-1, 1] 範圍
    cos_d = min(1, max(-1, cos_d))


    d = radius * math.acos(cos_d)

    return d




