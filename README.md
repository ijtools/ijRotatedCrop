# Rotated Crop Plugin 
Crop 2D/3D images with various bosx orientations.

The RotCrop Plugins for ImageJ allow to perform rotated crop of 2D/3D images.
In fully manual versions, the crop can be defined based on crop dimensions, crop center, 
and rotation angle(s) of the crop frame (in 3D, three angles are necessary).

Additional plugins provides an estimate of the rotation based on the gradient vector
computed around the crop frame center. The gradient direction is used as vertical direction 
in the result, making it easier to generate 2D/3D images that are tangent to a surface such as
the epidermis of an organ or organism.


## Installation

The plugin requires two additonal libraries to work properly:

* [ijGeometry](https://github.com/ijtools/ijGeometry)
* [ijRegister](https://github.com/ijtools/ijRegister)

First, copy the jar files of the dependencies into the "plugins/jar" directory of your ImageJ/Fiji installation.
Then, copy the jar file of the "RotatedCrop" plugin into the "plugins" directory.
Finally, restart ImageJ

The new plugins will be available within the "Plugins->INRAE->Rot Crop" menu.


