## Local Development

This repo was created at [this link](https://app.netlify.com/start/deploy?repository=https://github.com/netlify-templates/one-click-hugo-cms&stack=cms).

You'll need to set up [Netlify CI & Dev](https://docs.netlify.com/cli/get-started/#installation).
1. `npm install netlify-cli -g`
2. `netlify login`
	3.1 This will open in a browser window. Click the authorize button.
3. `netlify init`

Once that's set up, run the server with `netlify dev`.
For more about how to use the Netlify CLI, use the commands `netlify` and `netlify help`.

## Deploying to Netlify

The Netlify site can be found [here](https://app.netlify.com/sites/stupefied-goldberg-35c5b7/overview).

Pushing to this repo will automatically start a deploy there.

## Creating Microsites
Log into the admin UI [here](https://value.hexawise.com/admin).

Click on the 'Create Microsite' button to make a new microsite, or click on any of the existing ones to make edits.

Once you've created or made edits to a microsite, there will be a push to this repo with your changes, so remember to pull afterwards.

## Adding Images & Videos
Adding images to a microsite is as easy as uploading them through the admin UI.

To add videos, you'll need to upload them to our `media.hexawise.com` S3 bucket first.

[Cyberduck](https://cyberduck.io/) is a simple way to upload videos. Ping me for the secret keys needed to use the `media-uploads` IAM user.
1. Open Cyberduck.
2. Click on the 'Open Connection' icon.
3. Select 'Amazon S3' from the dropdown menu at the top of the dialog.
4. Enter the Access Key ID and Secret Access Key for the `media-uploads` user.
5. Click 'Connect'.
6. You should see a list of multiple S3 buckets. The one you're looking for is as at the bottom of the list, `media.hexawise.com`.
7. In that dropdown list, find the `video` folder. This is where you'll add your video(s).

Once the video has been uploaded, you can include its URL(which should look like `https://s3.amazonaws.com/media.hexawise.com/video/video-filename.mp4`) in a Section Video field for your microsite.
