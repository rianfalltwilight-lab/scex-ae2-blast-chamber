$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$taskRoot = Split-Path $PSScriptRoot -Parent
foreach ($name in @('atlas','active')) {
    $inputImage = [System.Drawing.Image]::FromFile((Join-Path $taskRoot "art/chamber-$name-original.png"))
    $texture = New-Object System.Drawing.Bitmap 128,128
    $graphics = [System.Drawing.Graphics]::FromImage($texture)
    try {
        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
        $graphics.DrawImage($inputImage,[System.Drawing.Rectangle]::new(0,0,128,128),0,0,$inputImage.Width,$inputImage.Height,[System.Drawing.GraphicsUnit]::Pixel)
        $texture.Save((Join-Path $taskRoot "src/main/resources/assets/ae2blast/textures/block/chamber_$name.png"),[System.Drawing.Imaging.ImageFormat]::Png)
    } finally { $graphics.Dispose(); $texture.Dispose(); $inputImage.Dispose() }
}
