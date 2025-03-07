import argparse


class Options:
    def __init__(self, prog=None):
        # =====================
        # Image Detail
        # =====================
        self.imw = 1280
        self.imh = 512
        self.channel = 3
        self.resize = False
        # self.resize_scale = 2  # medium
        # self.resize_scale = 4  # small

        # =====================
        # Sh Detail
        # =====================
        self.nbands = 5
        self.coeff_len = self.nbands*self.nbands
        
        
        # =====================
        # Dataset
        # =====================
        self.workers = 0
        self.equi_coord = "../data/pano_coord_1024.npy"
        self.equi = True

        # Training Param #
        # self.train_path = '/home/juliussurya/work/cat2dog/train_b.txt'
        # self.train_path = ".\\pano_data_train"
        self.train_path = r"D:/datasets/360SP/360SP_train_offset_fc"
        # self.train_path = ".\\sun360_100"
        self.train_batch = 4
        self.train_shuffle = True
        # self.train_len = 39730
        self.train_len = 10000
        self.ckpt_path = ".\\ckpt"
        # self.model_path = '../trained_model'
        self.model_path = ".\\trained_model\\outdoor_offset_fc"
        self.learn_rate = 0.0002
        self.lr_d = 0.0004
        self.lr_g = 0.0002
        self.beta1 = 0.5
        self.beta2 = 0.999
        # self.total_epochs = 2000
        self.total_epochs = 2000
        self.train_log = ".\\log\\outdoor_offsetFC_small"
        self.print_step = 500

        # Test Param #
        self.test_path = ".\\360dataset\\pano_data_val"
        self.test_batch = 1
        self.test_shuffle = False
        self.test_len = 5000
        self.output_path = ".\\output"

        # Val param #
        self.val_path = ".\\360dataset\\pano_data_val"
        self.val_batch = 1
        self.val_shuffle = True
        self.val_len = 5000

        self.net = None

        self.parser = argparse.ArgumentParser(prog=prog)
        self._define_parser()
        self._parse()
        # =====================
        args = self.parser.parse_args()
        self.net = args.net
        if args.net=="small":
            self.resize_scale = 4  # small
        elif args.net=="medium":
            self.resize_scale = 2  # medium
            
        self.two_end_align = True

    def _define_parser(self):
        self.parser.add_argument(
            "--net",
            default="medium",  # soojie: train medium net
            # self.parser.add_argument('--net', default='large', #soojie: train large net
            # self.parser.add_argument('--net', default='fov',  #soojie: train fov
            help="Network type (small || medium || large",
        )

    def _parse(self):
        args = self.parser.parse_args()
        self.net = args.net
